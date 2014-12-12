package computer;

import voce.SpeechInterface;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CommandTree {

    public CommandTree(){}

    private WordNode root = new WordNode("");
    public void addCommand( String command, Action action ) {
        System.out.println("Adding '" + command + "' to the command tree.");
        List<String> sentence = new ArrayList<String>(Arrays.asList(command.split(" ")));
        root.attachAction(sentence, action);
    }

    public void runCommand( String command ) {
        root.runAction(new ArrayList<String>(Arrays.asList(command.split(" "))));
    }

    public void printTree() {
        root.printTree("");
    }

    public static interface  Action {
        /**
         * Performs the action
         */
        void run();
    }
    public static class MessageAction implements Action {
        public MessageAction(String message) {
            this.message = message;
        }
        private String message;
        public void run() {
            SpeechInterface.synthesize(message);
            System.out.println(message);
        }
    }

    public static class ExternalAction implements Action {
        private String message;
        private String path;
        public ExternalAction(String message, String path) {
            this.message = message;
            this.path = path;
        }

        public void run() {
            SpeechInterface.synthesize(message);
            String fullPath = System.getProperty("user.dir") + "\\commands\\" + path;
            try {
                System.out.println("Executing:\n" + fullPath);
                Runtime.getRuntime().exec("CMD /c " + fullPath);
            } catch (Exception e ) {
                SpeechInterface.synthesize("I have failed to do your bidding. I apologize.");
                System.out.println(e.getMessage());
            }
        }
    }

    public static class WordNode {
        String word;
        List<WordNode> children;
        Action action = null;
        public WordNode( String word ) {
            this.word = word;
            children = new ArrayList<WordNode>();
        }
        public WordNode( String word, List<WordNode> children ) {
            this.word = word;
            this.children = children;
        }

        public void printTree(String prefix) {
            if  ( action != null ) {
                System.out.println(prefix + word + " (ACTIONABLE)");
            } else {
                System.out.println(prefix + word);
            }

            for ( WordNode child : children ) {
                child.printTree("--" + prefix);
            }
        }

        public void runAction( List<String> sentence ) {
            if ( sentence.size() == 0 ) {
                if (action != null ) {
                    action.run();
                } else {
                    // TODO - Handle without nulls - maybe a non-actionable command.
                    System.out.println("Not an actionable command.");
                }
                return;
            }
            String nextWord = sentence.remove(0);
            for ( WordNode child : children ) {
                if (child.word.equals(nextWord)) {
                    child.runAction(sentence);
                    return;
                }
            }
            System.out.println("Command not in command tree.");
        }

        public void attachAction( List<String> sentence, Action action ) {
            if (sentence.size() == 0) {
                this.action = action;
                // TODO - check for multiple actions and handle appropriately
                return;
            } else {
                for ( WordNode child : children ) {
                    if (child.word.equals(sentence.get(0)) ) {
                        sentence.remove(0);
                        child.attachAction(sentence, action);
                        return;
                    }
                }
            }
            String nextWord = sentence.remove(0);
            WordNode nextChild = new WordNode(nextWord);
            nextChild.attachAction(sentence, action);
            children.add(nextChild);
        }
    }
}
