package xyz.jmburns.passgen.application;

import org.apache.commons.cli.Options;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;

import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.ResourceBundle;

import xyz.jmburns.passgen.api.PasswordGenerator;

import static java.lang.Integer.parseInt;

class Main {
    private static final ResourceBundle MESSAGES = ResourceBundle.getBundle("messages");
    private static final Options SUPPORTED_OPTIONS;
    private static final int SUCCESS = 0;
    private static final int ERROR = 2;
    private static final int DEFAULT_MAXIMUM_LENGTH = 15;

    private final LocalisedConsole console;
    private final CommandLine arguments;

    static {
        SUPPORTED_OPTIONS = new Options().addOption(
                "h",
                "help",
                false,
                MESSAGES.getString("help.option.help")
        ).addOption(
                "e",
                "example",
                false,
                MESSAGES.getString("help.option.example")
        ).addOption(
                "l",
                "max-length",
                true,
                MESSAGES.getString("help.option.max_length")
        );
    }

    private Main(CommandLine arguments) {
        this.console = new LocalisedConsole(MESSAGES);
        this.arguments = arguments;
    }    
    
    private void run() {
        Optional<String> result = Optional.empty();
        if (arguments.hasOption("help")) {
            handleHelpRequest();
        } else if (arguments.hasOption("example")) {
            handleExampleExample();
        } else if (!arguments.getArgList().isEmpty()) {
            result = Optional.of(generateFromArguments());
        } else {
            result = Optional.of(generateFromPrompts());
        }

        result.ifPresent(this::displayResult);
        System.exit(SUCCESS);
    }

    private void handleHelpRequest() {
        new HelpFormatter().printHelp(
                MESSAGES.getString("help.usage"),
                MESSAGES.getString("help.header"),
                SUPPORTED_OPTIONS,
                MESSAGES.getString("help.footer"),
                false
        );
    }

    private void handleExampleExample() {
        console.displayAllLines("help.example", "%n");
    }

    private String generateFromArguments() {
        int maximumLength = DEFAULT_MAXIMUM_LENGTH;

        try {
            String maximumLengthArgument = arguments.getOptionValue("max-length");

            if (maximumLengthArgument != null) {
                maximumLength = parseInt(maximumLengthArgument);
            }
        } catch (NumberFormatException invalidInput) {
            console.display("error.invalid_length_option");
            System.exit(ERROR);
        }

        return PasswordGenerator.generate(arguments.getArgList(), maximumLength);
    }

    private String generateFromPrompts() {
        console.display("message.welcome");
        console.nextLine();

        return PasswordGenerator.generate(
                promptForPhrase(),
                promptForMaximumPasswordLength()
        );
    }

    private int promptForMaximumPasswordLength() {
        OptionalInt length = console.promptForOptionalInt(
                "question.password_length",
                "error.invalid_integer"
        );

        return length.orElse(DEFAULT_MAXIMUM_LENGTH);
    }

    private List<String> promptForPhrase() {
        List<String> phrase = new ArrayList<>();

        String websiteName = console.promptForString(
                "question.website_name",
                "error.required_answer"
        );
        phrase.add(websiteName);

        phrase.addAll(console.promptForAllOptionalStrings(
                "question.security_questions",
                "&")
        );

        return phrase;
    }

    private void displayResult(String password) {
        String message = MESSAGES.getString("message.result") + " " + password;
        console.displayRaw(message);

        String shouldCopy = console.promptForString("message.clipboard");
        if (shouldCopy.toLowerCase().charAt(0) == 'y') {
            copyToClipboard(password);
        }
    }

    private void copyToClipboard(String password) {
        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        StringSelection data = new StringSelection(password);
        clipboard.setContents(data, null);
    }

    public static void main(String[] arguments) throws Exception {
        CommandLine parsedArguments = new DefaultParser().parse(SUPPORTED_OPTIONS, arguments);
        new Main(parsedArguments).run();
    }
}
