/*
  * Anserini: A Lucene toolkit for reproducible information retrieval research
  *
  * Licensed under the Apache License, Version 2.0 (the "License");
  * you may not use this file except in compliance with the License.
  * You may obtain a copy of the License at
  *
  * http://www.apache.org/licenses/LICENSE-2.0
  *
  * Unless required by applicable law or agreed to in writing, software
  * distributed under the License is distributed on an "AS IS" BASIS,
  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  * See the License for the specific language governing permissions and
  * limitations under the License.
  */
 
 package io.anserini.search;
 
 import io.anserini.analysis.AnalyzerUtils;
 import io.anserini.index.IndexArgs;
 import io.anserini.index.IndexCollection;
 import io.anserini.index.IndexReaderUtils;
 import io.anserini.rerank.RerankerCascade;
 import io.anserini.rerank.RerankerContext;
 import io.anserini.rerank.ScoredDocuments;
 import io.anserini.rerank.lib.Rm3Reranker;
 import io.anserini.rerank.lib.ScoreTiesAdjusterReranker;
 import io.anserini.search.query.BagOfWordsQueryGenerator;
 import io.anserini.search.query.QueryGenerator;
 import io.anserini.search.topicreader.TopicReader;
 import org.apache.commons.lang3.time.DurationFormatUtils;
 import org.apache.logging.log4j.LogManager;
 import org.apache.logging.log4j.Logger;
 import org.apache.lucene.analysis.Analyzer;
 import org.apache.lucene.analysis.ar.ArabicAnalyzer;
 import org.apache.lucene.analysis.bn.BengaliAnalyzer;
 import org.apache.lucene.analysis.cjk.CJKAnalyzer;
 import org.apache.lucene.analysis.da.DanishAnalyzer;
 import org.apache.lucene.analysis.de.GermanAnalyzer;
 import org.apache.lucene.analysis.es.SpanishAnalyzer;
 import org.apache.lucene.analysis.fi.FinnishAnalyzer;
 import org.apache.lucene.analysis.fr.FrenchAnalyzer;
 import org.apache.lucene.analysis.hi.HindiAnalyzer;
 import org.apache.lucene.analysis.hu.HungarianAnalyzer;
 import org.apache.lucene.analysis.id.IndonesianAnalyzer;
 import org.apache.lucene.analysis.it.ItalianAnalyzer;
 import org.apache.lucene.analysis.ja.JapaneseAnalyzer;
 import org.apache.lucene.analysis.nl.DutchAnalyzer;
 import org.apache.lucene.analysis.no.NorwegianAnalyzer;
 import org.apache.lucene.analysis.pt.PortugueseAnalyzer;
 import org.apache.lucene.analysis.ru.RussianAnalyzer;
 import org.apache.lucene.analysis.sv.SwedishAnalyzer;
 import org.apache.lucene.analysis.th.ThaiAnalyzer;
 import org.apache.lucene.analysis.tr.TurkishAnalyzer;
 import org.apache.lucene.analysis.core.WhitespaceAnalyzer;
 
 import org.apache.lucene.document.Document;
 import org.apache.lucene.index.DirectoryReader;
 import org.apache.lucene.index.IndexReader;
 import org.apache.lucene.index.IndexableField;
 import org.apache.lucene.search.IndexSearcher;
 import org.apache.lucene.search.Query;
 import org.apache.lucene.search.Sort;
 import org.apache.lucene.search.SortField;
 import org.apache.lucene.search.TopDocs;
 import org.apache.lucene.search.similarities.BM25Similarity;
 import org.apache.lucene.search.similarities.LMDirichletSimilarity;
 import org.apache.lucene.search.similarities.Similarity;
 import org.apache.lucene.store.FSDirectory;
 import org.kohsuke.args4j.CmdLineException;
 import org.kohsuke.args4j.CmdLineParser;
 import org.kohsuke.args4j.Option;
 import org.kohsuke.args4j.OptionHandlerFilter;
 import org.kohsuke.args4j.ParserProperties;
 
 import java.io.Closeable;
 import java.io.IOException;
 import java.io.PrintWriter;
 import java.nio.charset.StandardCharsets;
 import java.nio.file.Files;
 import java.nio.file.Path;
 import java.nio.file.Paths;
 import java.util.ArrayList;
 import java.util.Arrays;
 import java.util.HashMap;
 import java.util.List;
 import java.util.Locale;
 import java.util.Map;
 import java.util.SortedMap;
 import java.util.concurrent.CompletionException;
 import java.util.concurrent.ConcurrentHashMap;
 import java.util.concurrent.Executors;
 import java.util.concurrent.ThreadPoolExecutor;
 import java.util.concurrent.TimeUnit;
 import java.util.concurrent.atomic.AtomicLong;
 
 /**
  * Class that exposes basic search functionality, designed specifically to provide the bridge between Java and Python
  * via pyjnius.
  */
 public class SimpleSearcher implements Closeable {
   public static final Sort BREAK_SCORE_TIES_BY_DOCID =
       new Sort(SortField.FIELD_SCORE, new SortField(IndexArgs.ID, SortField.Type.STRING_VAL));
   private static final Logger LOG = LogManager.getLogger(SimpleSearcher.class);
 
   public static final class Args {
     @Option(name = "-index", metaVar = "[path]", required = true, usage = "Path to Lucene index.")
     public String index;
 
     @Option(name = "-topics", metaVar = "[file]", required = true, usage = "Topics file.")
     public String topics;
 
     @Option(name = "-output", metaVar = "[file]", required = true, usage = "Output run file.")
     public String output;
 
     @Option(name = "-bm25", usage = "Flag to use BM25.", forbids = {"-ql"})
     public Boolean useBM25 = true;
 
     @Option(name = "-bm25.k1", usage = "BM25 k1 value.", forbids = {"-ql"})
     public float bm25_k1 = 0.9f;
 
     @Option(name = "-bm25.b", usage = "BM25 b value.", forbids = {"-ql"})
     public float bm25_b = 0.4f;
 
     @Option(name = "-qld", usage = "Flag to use query-likelihood with Dirichlet smoothing.", forbids={"-bm25"})
     public Boolean useQL = false;
 
     @Option(name = "-qld.mu", usage = "Dirichlet smoothing parameter value for query-likelihood.", forbids={"-bm25"})
     public float ql_mu = 1000.0f;
 
     @Option(name = "-rm3", usage = "Flag to use RM3.")
     public Boolean useRM3 = false;
 
     @Option(name = "-rm3.fbTerms", usage = "RM3 parameter: number of expansion terms")
     public int rm3_fbTerms = 10;
 
     @Option(name = "-rm3.fbDocs", usage = "RM3 parameter: number of documents")
     public int rm3_fbDocs = 10;
 
     @Option(name = "-rm3.originalQueryWeight", usage = "RM3 parameter: weight to assign to the original query")
     public float rm3_originalQueryWeight = 0.5f;
 
     @Option(name = "-hits", metaVar = "[number]", usage = "Max number of hits to return.")
     public int hits = 1000;
 
     @Option(name = "-threads", metaVar = "[number]", usage = "Number of threads to use.")
     public int threads = 1;
 
     @Option(name = "-language", usage = "Analyzer Language")
     public String language = "en";
   }
 
   protected IndexReader reader;
   protected Similarity similarity;
   protected Analyzer analyzer;
   protected RerankerCascade cascade;
   protected boolean useRM3;
 
   protected IndexSearcher searcher = null;
 
   /**
    * This class is meant to serve as the bridge between Anserini and Pyserini.
    * Note that we are adopting Python naming conventions here on purpose.
    */
   public class Result {
     public String docid;
     public int lucene_docid;
     public float score;
     public String contents;
     public String raw;
     public Document lucene_document; // Since this is for Python access, we're using Python naming conventions.
 
     public Result(String docid, int lucene_docid, float score, String contents, String raw, Document lucene_document) {
       this.docid = docid;
       this.lucene_docid = lucene_docid;
       this.score = score;
       this.contents = contents;
       this.raw = raw;
       this.lucene_document = lucene_document;
     }
   }
 
   protected SimpleSearcher() {
   }
 
   /**
    * Creates a {@code SimpleSearcher}.
    *
    * @param indexDir index directory
    * @throws IOException if errors encountered during initialization
    */
   public SimpleSearcher(String indexDir) throws IOException {
     this(indexDir, IndexCollection.DEFAULT_ANALYZER);
   }
 
   /**
    * Creates a {@code SimpleSearcher} with a specified analyzer.
    *
    * @param indexDir index directory
    * @param analyzer analyzer to use
    * @throws IOException if errors encountered during initialization
    */
   public SimpleSearcher(String indexDir, Analyzer analyzer) throws IOException {
     Path indexPath = Paths.get(indexDir);
 
     if (!Files.exists(indexPath) || !Files.isDirectory(indexPath) || !Files.isReadable(indexPath)) {
       throw new IllegalArgumentException(indexDir + " does not exist or is not a directory.");
     }
 
     SearchArgs defaults = new SearchArgs();
 
     this.reader = DirectoryReader.open(FSDirectory.open(indexPath));
     // Default to using BM25.
     this.similarity = new BM25Similarity(Float.parseFloat(defaults.bm25_k1[0]), Float.parseFloat(defaults.bm25_b[0]));
     this.analyzer = analyzer;
     this.useRM3 = false;
     cascade = new RerankerCascade();
     cascade.add(new ScoreTiesAdjusterReranker());
   }
 
   /**
    * Sets the analyzer used.
    *
    * @param analyzer analyzer to use
    */
   public void setAnalyzer(Analyzer analyzer) {
     this.analyzer = analyzer;
   }
 
   /**
    * Returns the analyzer used.
    *
    * @return analyzed used
    */
   public Analyzer getAnalyzer(){
     return this.analyzer;
   }
 
   /**
    * Sets the language.
    *
    * @param language language
    */
   public void setLanguage(String language) {
     if (language.equals("ar")) {
       this.analyzer = new ArabicAnalyzer();
     } else if (language.equals("bn")) {
       this.analyzer = new BengaliAnalyzer();
     } else if (language.equals("de")) {
       this.analyzer = new GermanAnalyzer();
     } else if (language.equals("da")) {
       this.analyzer = new DanishAnalyzer();
     } else if (language.equals("es")) {
       this.analyzer = new SpanishAnalyzer();
     } else if (language.equals("fi")) {
       this.analyzer = new FinnishAnalyzer();
     } else if (language.equals("fr")) {
       this.analyzer = new FrenchAnalyzer();
     } else if (language.equals("hi")) {
       this.analyzer = new HindiAnalyzer();
     } else if (language.equals("hu")) {
       this.analyzer = new HungarianAnalyzer();
     } else if (language.equals("id")) {
       this.analyzer = new IndonesianAnalyzer();
     } else if (language.equals("it")) {
       this.analyzer = new ItalianAnalyzer();
     } else if (language.equals("ja")) {
       this.analyzer = new JapaneseAnalyzer();
     } else if (language.equals("nl")) {
       this.analyzer = new DutchAnalyzer();
     } else if (language.equals("no")) {
       this.analyzer = new NorwegianAnalyzer();
     } else if (language.equals("pt")) {
       this.analyzer = new PortugueseAnalyzer();
     } else if (language.equals("ru")) {
       this.analyzer = new RussianAnalyzer();
     } else if (language.equals("sv")) {
       this.analyzer = new SwedishAnalyzer();
     } else if (language.equals("th")) {
       this.analyzer = new ThaiAnalyzer();
     } else if (language.equals("tr")) {
       this.analyzer = new TurkishAnalyzer();
     } else if (language.equals("zh") || language.equals("ko")) {
       this.analyzer = new CJKAnalyzer();
     } else if (language.equals("sw") || language.equals("te")) {
       this.analyzer = new WhitespaceAnalyzer();
       // For Mr.TyDi: sw and te do not have custom Lucene analyzers, so just use whitespace analyzer.
     }
   }
 
   /**
    * Returns whether or not RM3 query expansion is being performed.
    *
    * @return whether or not RM3 query expansion is being performed
    */
   public boolean useRM3() {
     return useRM3;
   }
 
   /**
    * Disables RM3 query expansion.
    */
   public void unsetRM3() {
     this.useRM3 = false;
     cascade = new RerankerCascade();
     cascade.add(new ScoreTiesAdjusterReranker());
   }
 
   /**
    * Enables RM3 query expansion with default parameters.
    */
   public void setRM3() {
     SearchArgs defaults = new SearchArgs();
     setRM3(Integer.parseInt(defaults.rm3_fbTerms[0]), Integer.parseInt(defaults.rm3_fbDocs[0]),
         Float.parseFloat(defaults.rm3_originalQueryWeight[0]));
   }
 
   /**
    * Enables RM3 query expansion with default parameters.
    *
    * @param fbTerms number of expansion terms
    * @param fbDocs number of expansion documents
    * @param originalQueryWeight weight to assign to the original query
    */
   public void setRM3(int fbTerms, int fbDocs, float originalQueryWeight) {
     setRM3(fbTerms, fbDocs, originalQueryWeight, false, true);
   }
 
   /**
    * Enables RM3 query expansion with default parameters.
    *
    * @param fbTerms number of expansion terms
    * @param fbDocs number of expansion documents
    * @param originalQueryWeight weight to assign to the original query
    * @param outputQuery flag to print original and expanded queries
    * @param filterTerms whether to filter terms to be English only
    */
   public void setRM3(int fbTerms, int fbDocs, float originalQueryWeight, boolean outputQuery, boolean filterTerms) {
     useRM3 = true;
     cascade = new RerankerCascade("rm3");
     cascade.add(new Rm3Reranker(this.analyzer, IndexArgs.CONTENTS,
         fbTerms, fbDocs, originalQueryWeight, outputQuery, filterTerms));
     cascade.add(new ScoreTiesAdjusterReranker());
   }
 
   /**
    * Specifies use of query likelihood with Dirichlet smoothing as the scoring function.
    *
    * @param mu mu smoothing parameter
    */
   public void setQLD(float mu) {
     this.similarity = new LMDirichletSimilarity(mu);
 
     // We need to re-initialize the searcher
     searcher = new IndexSearcher(reader);
     searcher.setSimilarity(similarity);
   }
 
   /**
    * Specifies use of BM25 as the scoring function.
    *
    * @param k1 k1 parameter
    * @param b b parameter
    */
   public void setBM25(float k1, float b) {
     this.similarity = new BM25Similarity(k1, b);
 
     // We need to re-initialize the searcher
     searcher = new IndexSearcher(reader);
     searcher.setSimilarity(similarity);
   }
 
   /**
    * Returns the {@link Similarity} (i.e., scoring function) currently being used.
    *
    * @return the {@link Similarity} currently being used
    */
   public Similarity getSimilarity() {
     return similarity;
   }
 
   /**
    * Returns the number of documents in the index.
    *
    * @return the number of documents in the index
    */
    public int getTotalNumDocuments(){
      // Create an IndexSearch only once. Note that the object is thread safe.
      if (searcher == null) {
        searcher = new IndexSearcher(reader);
        searcher.setSimilarity(similarity);
      }
 
      return searcher.getIndexReader().maxDoc();
    }
 
   /**
    * Closes this searcher.
    */
   @Override
   public void close() throws IOException {
     try {
       reader.close();
     } catch (Exception e) {
       // Eat any exceptions.
       return;
     }
   }
 
   /**
    * Searches the collection using multiple threads.
    *
    * @param queries list of queries
    * @param qids list of unique query ids
    * @param k number of hits
    * @param threads number of threads
    * @return a map of query id to search results
    */
   public Map<String, Result[]> batchSearch(List<String> queries, List<String> qids, int k, int threads) {
     QueryGenerator generator = new BagOfWordsQueryGenerator();
     return batchSearchFields(generator, queries, qids, k, threads, new HashMap<>());
   }
 
   /**
    * Searches the collection using multiple threads.
    *
    * @param generator the method for generating queries
    * @param queries list of queries
    * @param qids list of unique query ids
    * @param k number of hits
    * @param threads number of threads
    * @return a map of query id to search results
    */
   public Map<String, Result[]> batchSearch(QueryGenerator generator, List<String> queries, List<String> qids, int k, int threads) {
     return batchSearchFields(generator, queries, qids, k, threads, new HashMap<>());
   }
 
 
/** Searches the provided fields weighted by their boosts, using multiple threads. */

public Map<String, Result[]> batchSearchFields(List<String> queries, List<String> qids, int k, int threads, Map<String, Float> fields) {
    if (searcher == null) {
        searcher = new IndexSearcher(reader);
        searcher.setSimilarity(similarity);
    }

    ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(threads);
    Map<String, Result[]> results = new ConcurrentHashMap<>();

    for (int i = 0; i < queries.size(); i++) {
        String query = queries.get(i);
        String qid = qids.get(i);

        executor.execute(() -> {
            try {
                Query luceneQuery = generator.buildQuery(IndexArgs.CONTENTS, analyzer, query);
                TopDocs topDocs = searcher.search(luceneQuery, k, BREAK_SCORE_TIES_BY_DOCID);

                Result[] queryResults = new Result[topDocs.scoreDocs.length];
                for (int j = 0; j < topDocs.scoreDocs.length; j++) {
                    int docId = topDocs.scoreDocs[j].doc;
                    Document doc = searcher.doc(docId);
                    float score = topDocs.scoreDocs[j].score;
                    String contents = doc.get(IndexArgs.CONTENTS);
                    String raw = doc.get(IndexArgs.RAW);

                    queryResults[j] = new Result(doc.get(IndexArgs.ID), docId, score, contents, raw, doc);
                }

                results.put(qid, queryResults);
            } catch (IOException e) {
                LOG.error("Error searching for query: " + query, e);
            }
        });
    }

    executor.shutdown();
    try {
        executor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
    } catch (InterruptedException e) {
        LOG.error("Error waiting for search threads to finish.", e);
    }

    return results;
}
 

}