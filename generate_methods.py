import os
import sys
import re
import shlex
import pandas as pd
import json
from openai import OpenAI
from tenacity import retry, wait_exponential, stop_after_attempt
from tqdm import tqdm
from concurrent.futures import ThreadPoolExecutor, as_completed
import threading
import logging
import tiktoken
import subprocess
from dotenv import load_dotenv

# Configure logging
logging.basicConfig(filename='terminal_output.log', level=logging.INFO)

# Set up OpenAI API credentials
os.environ["OPENAI_API_KEY"] = API_KEY

# Define the model
client = OpenAI()
model = "gpt-3.5-turbo-16k"

# Define the path for methods
cwd = os.getcwd()
dest = r"Results\Non-Full-Context\Copilot-Run\NonFullContext"
folder_path = os.path.join(cwd, dest)

# Navigate two levels up to find metadata csv file
two_levels_up = os.path.dirname(os.path.dirname(folder_path))
file_path = os.path.join(two_levels_up, "NonFullContext-With-Stats.csv")

# Load the CSV file
reference_file = pd.read_csv(file_path)

# Navigate four levels up to find instances csv file to write
instances_path = os.path.join(cwd, "instances_copy.csv")
df = pd.read_csv(instances_path, index_col="index")

# Get a list of folders in the path
folders = os.listdir(folder_path)

# Regular expression to match Java method declarations
# method_regex  = r"(?:(?:public|private|protected|static|final|native|synchronized|abstract|transient)+\s+)+[$_\w<>\[\]\s]*\s+[\$_\w]+\([^\)]*\)?\s*\{?[^\}]*\}?"
method_regex = r"(?:(?<=\/))?(?:(?:void|boolean|default|public|private|protected|static|final|native|synchronized|abstract|transient)+\s+)*(?!j)(?!a)(?!v)(?!a)[$_\w<>,.?\[\]\s]*\s*[\$_\w]*\([^\)]*\)?\s*\{?[^\`]*\}?"

input_files = ["Original.java", "PerturbedEvaluator.java", "PerturbedPegasus.java", "PerturbedPivoting.java"]
output_files = ["NewResultOriginal.java", "NewResultPerturbedEvaluator.java", "NewResultPerturbedPegasus.java", "NewResultPerturbedPivoting.java"]


@retry(wait=wait_exponential(multiplier=1, min=2, max=25), stop=stop_after_attempt(30))
def call_model(client, upper_context, method_comment, method_sign):
    chat_completion = client.chat.completions.create(
            model=model,
            messages=[
                {
                    "role": "system", 
                    "content": "You are a helpful code assistant. Your language of choice is Java. Generate the code block for the mentioned method with full code and logic."
                    },
                {
                    "role": "user",
                    "content": "Write a valid Java method with implementation logic whose preceding code context is: " + upper_context + 
                                ",whose comment is : " + method_comment + 
                                "and the method signature is: " + method_sign
                    }
            ],
            seed=42,
            temperature=0,
        )
    return chat_completion


def extract_method(generated_code, search_result):
    extracted_method = ""
    # Every method in the LLM repsonse starts with this keyword
    keyword = "```java"
    comment_end = "*/" 

    if search_result:
        start_index = search_result.start()

        if keyword in generated_code:
            start_index = generated_code.index(keyword) + len(keyword)
            if comment_end in generated_code:
                start_index = generated_code.index(comment_end) + len(comment_end)

        counter = 1  # Start after finding the opening brace of the method
        i = start_index
        first_open_brace = False

        while i < len(generated_code) and counter > 0:
            if generated_code[i] == '{':
                if not first_open_brace:
                    first_open_brace = True
                else:
                    counter += 1
            elif generated_code[i] == '}':
                counter -= 1
            i += 1

        extracted_method = generated_code[start_index:i]
        print("Extracted Method: \n", extracted_method)
        logging.info("Extracted Method: \n" + extracted_method)
    else:
        return "fail"
    return extracted_method


def check_validity(extracted_method):

    if extracted_method == "fail":
        return -3   # no method found

    # Tracks whether the current character is inside a comment
    flag_quote = False
    # Indicates whether a valid method body has been found
    flag_valid = False
    # Tracks the balance of opening { and closing } braces
    counter = 0
    span_to_line = 0

    # Index and line number of opening brace(ob)  
    ob_index = extracted_method.find('{')
    if ob_index != -1:
        ob_line_number = extracted_method.count('\n', 0, ob_index) + 1
    else:
        return -1   # invalid method
    
    # Extracts method body
    from_to = '\n'.join(extracted_method.splitlines()[1:])
    # extracted_method = search_result.group()
    
    try:
        # Finds the signature of the next method (if any) within the method body
        next_signature = method_regex.findall(from_to)[0].split('\n')[0]
    except Exception:
        next_signature = ""

    # Check if the extracted method is valid
    for method_line_number, line in enumerate(extracted_method.splitlines(), start=1):

        for character in line:

            if character == '"' and not flag_quote:
                flag_quote = True

            else:
                if character == '"' and flag_quote:
                    flag_quote = False

            if character == '{' and not flag_quote:
                counter += 1

            if character == '}' and not flag_quote:
                counter -= 1

        if counter == 0 and ''.join(line.strip().split()) != ''.join(next_signature.strip().split()) and method_line_number > ob_line_number:
            flag_valid = True
            break

        elif line.strip() and ''.join(line.strip().split()) == ''.join(next_signature.strip().split()):
            flag_valid = False
            break

        else:
            span_to_line += 1

    # Checks if method is empty
    flag_empty = True
    # Loop to check if there is any non-empty line in the method body
    for line in extracted_method.splitlines()[ob_line_number:span_to_line]:
        if line.strip() != '' and not line.strip().startswith('//') and not line.strip().startswith('/*') and not line.strip().startswith('/**'):
            flag_empty = False
            break
    
    if flag_empty:
        return -2
    if flag_valid:
        return span_to_line + 1
    if not flag_valid:
        return -1

def num_tokens_from_messages(messages, model="gpt-3.5-turbo-0613"):
    """Return the number of tokens used by a list of messages."""
    try:
        encoding = tiktoken.encoding_for_model(model)
    except KeyError:
        print("Warning: model not found. Using cl100k_base encoding.")
        encoding = tiktoken.get_encoding("cl100k_base")
    if model in {
        "gpt-3.5-turbo-0613",
        "gpt-3.5-turbo-16k-0613",
        "gpt-4-0314",
        "gpt-4-32k-0314",
        "gpt-4-0613",
        "gpt-4-32k-0613",
        }:
        tokens_per_message = 3
        tokens_per_name = 1
    elif model == "gpt-3.5-turbo-0301":
        tokens_per_message = 4  # every message follows <|start|>{role/name}\n{content}<|end|>\n
        tokens_per_name = -1  # if there's a name, the role is omitted
    elif "gpt-3.5-turbo" in model:
        print("Warning: gpt-3.5-turbo may update over time. Returning num tokens assuming gpt-3.5-turbo-0613.")
        return num_tokens_from_messages(messages, model="gpt-3.5-turbo-0613")
    elif "gpt-4" in model:
        print("Warning: gpt-4 may update over time. Returning num tokens assuming gpt-4-0613.")
        return num_tokens_from_messages(messages, model="gpt-4-0613")
    else:
        raise NotImplementedError(
            f"""num_tokens_from_messages() is not implemented for model {model}. See https://github.com/openai/openai-python/blob/main/chatml.md for information on how messages are converted to tokens."""
        )
    num_tokens = 0
    for message in messages:
        num_tokens += tokens_per_message
        for key, value in message.items():
            num_tokens += len(encoding.encode(value))
            if key == "name":
                num_tokens += tokens_per_name
    num_tokens += 3  # every reply is primed with <|start|>assistant<|message|>
    return num_tokens

# Using generators read each line of the file one at a time
def read_file_line_by_line(file_path):
    with open(file_path, "r", encoding='utf-8') as file:
        for line_number, line in enumerate(file, start=1):
            yield line_number, line

count = 0
context_tokens_used = 0
response_tokens_used = 0

# Iterate over each folder
for folder in folders:

    if folder == ".DS_Store":
        continue
    
    # Fetching method starting line from the metadata file
    method_line = reference_file.loc[reference_file['index'] == int(folder), 'spanMethod'].item().split("-")[0]
    method_line = int(method_line)

    # Fetching test file path from the metadata file
    file_original_path = reference_file.loc[reference_file['index'] == int(folder), 'absolutePath'].item()[1:].replace('/', os.sep)
    test_path = file_original_path.replace("main", "test")
    test_path = test_path.replace(".java", "Test.java")
    
    test_class_name = os.path.basename(test_path)
    
    for input_file, output_file in zip(input_files, output_files):
    
        file_path = os.path.join(folder_path, folder, input_file)
        
        file_content = ""
        lower_context = ""

        # Read the content of the Original.java file
        for line_number, line in read_file_line_by_line(file_path):
            # Context of the method
            if line_number <= method_line:
                file_content += line
            elif line_number == (method_line+1):
                # Method comment
                method_comment = line
            elif line_number == (method_line+2):
                # Method signature
                method_sign = line
            elif line_number > (method_line+2):
                lower_context += line

        # Remove single-line comments        
        upper_context = re.sub(r'//.*', '', file_content)

        # Remove multi-line comments
        upper_context = re.sub(r'/\*.*?\*/', '', upper_context, flags=re.DOTALL)

        # Remove empty lines
        upper_context = re.sub(r'\n\s*\n', '\n', upper_context)

        print("Folder: ", folder)
        print("File: ", input_file)
        print("Method Starting Line: ", method_line)
        print("Method Comment: ", method_comment)
        print("Method Signature: ", method_sign)
        logging.info("Folder: " + folder + "\nFile: " + input_file + "\nMethod Starting Line: " + str(method_line) + "\nMethod Comment: " + method_comment + "\nMethod Signature: " + method_sign)

        
        # Define list of messages for token calculation
        messages = [{"content1": upper_context}, {"content2": method_comment}, {"content3": method_sign}]

        # Calculate number of tokens
        tokens_used = num_tokens_from_messages(messages, model="gpt-3.5-turbo-0613")
        context_tokens_used += tokens_used
        print("Context tokens Used:", str(context_tokens_used))

        # Maximum context length limit is 16385, keeping last 16385 characters in case of exceeding the limit
        if len(upper_context + method_comment + method_sign) > 16350:
            upper_context = upper_context[-(16350-len(method_comment+method_sign)):]

        response = call_model(client, upper_context, method_comment, method_sign)
        generated_code = response.choices[0].message.content
        print("LLM o/p: \n", generated_code)
        logging.info("\nLLM o/p: " + generated_code)

        response_tokens_used += response.usage.prompt_tokens
        print("Response tokens Used: ", response_tokens_used)

        # Search for the method using the regex
        search_result = re.search(method_regex, generated_code)

        # Extract the method from the model's response
        extracted_method = extract_method(generated_code, search_result)
        
        # Check the generated method's validity
        method_status = check_validity(extracted_method)

        # Creating test class name and location
        test_output_name = "result_test_" + test_class_name.split('.')[0] + "_" + df.loc[int(folder), 'methodName'] + "_" + output_file.split('.')[0] + '.txt'
        test_output = os.path.join(cwd , "TestResults", test_output_name)

        # creating final test file path
        test_file_dest = os.path.join("Repos-Full-Context\Repos-Full-Context~\ReadyFullContext", df.loc[int(folder), 'project'])
        test_file_path = os.path.join(cwd, test_file_dest, test_path)
        file_complete_path = os.path.join(cwd, test_file_dest, file_original_path)
        
        if method_status == -1:
            df.loc[int(folder), output_file.split('.')[0]] = "Not Valid"
        elif method_status == -2:
            df.loc[int(folder), output_file.split('.')[0]] = "Empty Method"
        elif method_status == -3:
            df.loc[int(folder), output_file.split('.')[0]] = "No Method"
        else:
            df.loc[int(folder), output_file.split('.')[0]] = extracted_method[:32600].strip()   # Excel cell has a limit of 32767 characters
        
        output_file_path = os.path.join(folder_path, folder, output_file)

        with open(output_file_path, "w", encoding="utf-8") as java_file:
            java_file.write(file_content + method_comment + extracted_method + lower_context)
            
        # Running unit tests on generated code
        # try:
        # classpath = f"{os.path.dirname(os.path.dirname(os.path.dirname(os.path.dirname(file_complete_path))))};{os.path.dirname(os.path.dirname(os.path.dirname(os.path.dirname(test_file_path))))}"
        pom_file_dest = os.path.join(cwd, test_file_dest)
        classpath = f"{pom_file_dest}"
        # subprocess.run(['javac', '-cp', classpath, test_file_path])  # Compile the test file
        
        # result = subprocess.run(['java', '-cp', classpath, 'org.junit.runner.JUnitCore', test_class_name], capture_output=True, text=True)  # Run the tests

        # Compile the test file
        compile_process = subprocess.run(['mvn', '-f', pom_file_dest, 'compile', 'test-compile'], capture_output=True, text=True, shell=True)
        
        # Run the tests
        test_process = subprocess.run(['mvn', '-f', pom_file_dest, 'test', '-Dtest=' + test_class_name], capture_output=True, text=True, shell=True)

        # Store the test results
        if test_process.returncode == 0:
            test_results = "PASS"
        else:
            test_results = "NOT PASS"
        
        # Update the testResults column in the dataframe
        result_col_name = 'TestResults' + output_file.split('.')[0]
        df.loc[int(folder), result_col_name] = test_results

        with open(test_output, "w", encoding="utf-8") as f:
            f.write(test_process.stdout)
            
        # except Exception as e:
        #     print(f"Error running tests for method {folder}: {e}") 


df.to_csv(instances_path)

        

logging.info("\nContext Tokens Used: " + str(context_tokens_used))
logging.info("\nResponse Tokens Used: " + str(response_tokens_used))