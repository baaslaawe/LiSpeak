package computer;

import voce.SpeechInterface;

import java.util.List;

public class VoiceInterface {
    public static void main(String[] args) {

        GrammarParser grammarParser = new GrammarParser();
        System.out.println("Parsing grammar");
        grammarParser.ParseGrammar("commands/commands.augmented", "grammars/commands.gram");
        List<GrammarParser.CommandPair> commands =  grammarParser.getCommands();

        for (GrammarParser.CommandPair cmd : commands ) {
            System.out.println( cmd.toString() );
            CommandParser.addCommand(cmd);
        }
        CommandParser.printCommandTree();

        System.out.println(System.getProperty("user.dir"));
        SpeechInterface.init("./lib", true, true, "./grammars", "commands");
        boolean keepListening = true;

        while ( keepListening ) {
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                // Nothing to do
            }

            while ( SpeechInterface.getRecognizerQueueSize() > 0 ) {
                String recognized = SpeechInterface.popRecognizedString();

                if ( recognized.contains("end program") ) {
                    keepListening = false;
                }

                CommandParser.RunCommands( recognized );
            }
        }
        SpeechInterface.synthesize("Goodbye.");
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            // Nothing to do
        }

        SpeechInterface.destroy();
    }
}
