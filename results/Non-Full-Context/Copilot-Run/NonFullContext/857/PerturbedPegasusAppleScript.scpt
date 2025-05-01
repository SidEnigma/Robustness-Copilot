
    tell application "Visual Studio Code" to open file ":Users:luca:Public:Dataset:CopilotInstances:NonFullContext:857:PerturbedPegasus.java"
    delay 3.5
    tell application "System Events" to tell process "Visual Studio Code"

        delay 2.0
        keystroke "p" using command down
        delay 2.5
        keystroke "564"
        delay 3.0
        key code 36
        delay 3.5
        keystroke "e" using command down
        delay 1.0
        keystroke "b" using command down
        delay 1.0
        key code 36
        delay 20.0
        key code 48
        key code 48
        key code 48
        key code 48
        delay 3.5
        keystroke "s" using command down
        delay 3.0
        keystroke "ResultPerturbedPegasus.java"
        delay 3.5
        key code 36
        delay 3.5
        keystroke "w" using command down
        delay 1.5
        keystroke "q" using command down

    end tell
    