package computer;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GrammarParser {

    public List<CommandPair> getCommands() {
        List<CommandPair> result = new ArrayList<CommandPair>();
        for ( String headRule : headRules ) {
            result.addAll(rules.get(headRule).getCommands(new CommandPair("", new ArrayList<String>())));
        }
        return result;
    }

    public static class CommandPair {
        public String command;
        public List<String> commandArgs;

        public static CommandPair empty() {
            return new CommandPair("", new ArrayList<String>());
        }
        public CommandPair(String command, List<String> commandArgs) {
            this.command = command;
            this.commandArgs = commandArgs;
        }

        public CommandPair copy() {
            return new CommandPair(this.command, new ArrayList<String>(this.commandArgs));
        }
        public String toString() {
            String s = command + " -> " + "{ ";
            String commands = "";
            for ( String cmd : commandArgs ) {
                if ( commands.length() > 0 ) {
                    commands += ", ";
                }
                commands += cmd;
            }
            s += commands + " }";
            return s;
        }
    }

    Map<String, Rule> rules = new HashMap<String, Rule>();
    List<String> headRules = new ArrayList<String>();

    public class Rule {

        public String toString() {
            String s = isHead ? "*" : "";
            s += name + " ( " + branches.size() + " ) -> { ";
            String commands = "";
            for ( String cmd : commandArgs ) {
                if ( commands.length() > 0 ) {
                    commands += ", ";
                }
                commands += cmd;
            }
            s += commands + " }";
            return s;
        }

        public List<CommandPair> getCommands(CommandPair parent) {
            List<CommandPair> result = new ArrayList<CommandPair>();
            CommandPair nextParent = parent.copy();
            nextParent.commandArgs.addAll(commandArgs);
            for (Branch branch : branches ) {
                result.addAll(branch.getCommands(nextParent));
            }
            return result;
        }

        public Rule( String name, List<String> commandArgs, boolean isHead) {
            this.name = name;
            this.commandArgs = commandArgs;
            this.branches = new ArrayList<Branch>();
            this.isHead = isHead;
        }

        public void add( List<Branch> branches) {
            this.branches.addAll(branches);
        }

        public boolean isHead;
        public String name;
        public List<String> commandArgs;
        public List<Branch> branches;
    }

    public class Branch {

        public String toString() {
            String s = words + " -> { ";
            String commands = "";
            for ( String cmd : commandArgs ) {
                if ( commands.length() > 0 ) {
                    commands += ", ";
                }
                commands += cmd;
            }
            s += commands + " }";
            return s;
        }

        public List<CommandPair> getCommands( CommandPair parent ) {
            CommandPair nextParent = new CommandPair( parent.command, new ArrayList<String>(parent.commandArgs));
            nextParent.commandArgs.addAll(commandArgs);
            List<String> words = new ArrayList<String>(this.words);
            List<CommandPair> result = new ArrayList<CommandPair>();
            genCommandsRecursive( nextParent, words, result);
            return result;
        }

        public void genCommandsRecursive( CommandPair parent, List<String> words, List<CommandPair> result ) {
            if ( words.size() == 0 ) {
                result.add(parent);
                return;
            }

            String nextWord = words.remove(0);

            if ( ! nextWord.startsWith("->")) {
                CommandPair nextParent = parent.copy();
                if ( nextParent.command.length() > 0 ) nextParent.command += " ";
                nextParent.command += nextWord;
                genCommandsRecursive( nextParent, words, result);
            } else {
                String key = nextWord.substring(2);
                Rule nextRule = rules.get(key);
                List<CommandPair> commandPairs = nextRule.getCommands( parent.copy() );
                for ( CommandPair pair : commandPairs ) {
                    List<String> nextWords = new ArrayList<String>();
                    Branch temp = new Branch(new ArrayList<String>(words), new ArrayList<String>(pair.commandArgs));
                    result.addAll(temp.getCommands(new CommandPair(pair.command, new ArrayList<String>())));
                }
                return;
            }
        }

        public Branch( List<String> words, List<String> commandArgs) {
            this.words = words;
            this.commandArgs = commandArgs;
        }
        public List<String> words;
        public List<String> commandArgs;
    }

    List<String> parseCommands( String cmds) {
        List<String> commandArgs = new ArrayList<String>();
        boolean inWord = false;
        StringBuilder builder = new StringBuilder();
        for ( int i = 0; i < cmds.length(); ++i ) {
            if ( cmds.charAt(i) == '"') {
                if ( inWord ) {
                    inWord = false;
                    commandArgs.add(builder.toString());
                    builder = new StringBuilder();
                } else {
                    inWord = true;
                }
            } else {
                if (inWord) {
                    builder.append(cmds.charAt(i));
                }
            }
        }
        return commandArgs;
    }

    Rule parseRule( String rule ) {
        boolean isHeadNode = false;
        String[] sides = rule.split("=");
        String lhs = sides[0].trim();
        String rhs = sides[1].trim();
        if (lhs.startsWith("public")) {
            isHeadNode = true;
            lhs = lhs.substring(6, lhs.length());
        }

        int idStart = lhs.indexOf("<") + 1;
        int idEnd = lhs.indexOf(">");
        String id = lhs.substring(idStart, idEnd);
        List<String> cmdArgs = new ArrayList<String>();
        if (lhs.contains("{") && lhs.contains("}")) {
            int cmdStart = lhs.indexOf("{") + 1;
            int cmdEnd = lhs.indexOf("}");
            String cmds = lhs.substring(cmdStart, cmdEnd);
            cmdArgs = parseCommands(cmds);
        }
        Rule ruleObj = new Rule(id, cmdArgs, isHeadNode);

        rhs = rhs.trim().replaceAll(" +", " ");
        rhs = rhs.replaceAll("\\*", "");
        rhs = rhs.replaceAll(";", "");
        List<Branch> branches = parseRHS( rhs );
        ruleObj.add(branches);

        return ruleObj;
    }

    public List<Branch> parseRHS( String rhs ) {
        String[] branchStrings = rhs.split("\\|");
        List<Branch> branches = new ArrayList<Branch>();
        for ( String branchString : branchStrings ) {
            branches.add(parseBranch(branchString));
        }
        return branches;
    }

    public Branch parseBranch( String branch ) {
        List<String> argString = new ArrayList<String>();
        if ( branch.contains("{")) {
            if ( branch.contains("}")) {
                int argStart = branch.indexOf("{");
                int argEnd = branch.indexOf("}");
                String args = branch.substring(argStart + 1, argEnd);
                argString = parseCommands( args );
                branch = branch.substring(0,argStart);
            } else {
                System.out.print("Bad augmentation");
                System.exit(-1);
            }
        }
        List<String> words = new ArrayList<String>();
        String[] wordStrings = branch.trim().split(" ");
        for (String wordString : wordStrings ) {
            wordString = wordString.trim();
            if (wordString.startsWith("<")) {
                wordString = "->" + wordString.substring(1, wordString.indexOf(">"));
            }
            words.add(wordString);
        }

        return new Branch(words, argString);
    }

    /**
     * Remove annotations from a line if it has any.
     * @param line
     * @return
     */
    public String stripLine(String line) {
        while (line.contains("{")) {
            if (line.contains("}")) {
                int start = line.indexOf("{");
                int end = line.indexOf("}", start);
                line = line.substring(0, start) + line.substring(end + 1, line.length());
            } else {
                System.out.println("Mis-matched augmentation brackets, aborting.");
                System.exit(-1);
            }
        }
        return line;
    }

    public void ParseGrammar( String inFile, String outFile ) {
        try {
            File f = new File(outFile);
            if ( f.exists()) {
                f.delete();
            }
            Writer writer = null;
            BufferedReader in = new BufferedReader( new FileReader(inFile));
            writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(f), "utf-8"));
            while (in.ready()) {
                String line = in.readLine();

                // Line contains a definition - build a grammar node and search for augmentations
                if ( line.contains("=")) {
                    Rule rule = parseRule( line );
                    rules.put(rule.name, rule);
                    if ( rule.isHead ) {
                        headRules.add(rule.name);
                    }
                }
                // Write the de-augmented line to the regular grammar file for later parsing
                writer.write(stripLine(line) + System.getProperty("line.separator"));
            }
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
