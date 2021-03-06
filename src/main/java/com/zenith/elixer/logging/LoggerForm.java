package com.zenith.elixer.logging;

import com.zenith.elixer.logging.instruction.ListInstruction;
import com.zenith.elixer.logging.instruction.LoggerInstruction;
import com.zenith.elixer.logging.instruction.StringInstruction;
import com.zenith.elixer.specification.ElixerBuffer;

import java.io.IOException;
import java.util.ArrayList;

public class LoggerForm {

    private static final int MAX_CHARACTER_LENGTH = Integer.MAX_VALUE;

    public static final LoggerForm DEFAULT = new LoggerForm("[{N} : {L}]| {LST( | )}");

    private ArrayList<LoggerInstruction> instructions = new ArrayList<>();
    private String formString;

    public LoggerForm(String formString) {
        this.formString = formString + '\0';
        try {
            parse();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void parse() throws IOException {
        ElixerBuffer b = new ElixerBuffer(formString);
        String currString = "";
        char currChar;
        for(int i = 0; i < MAX_CHARACTER_LENGTH; i++) {
            currChar = b.readChar();
            if(currChar == '\0')
                break;
            switch(currChar) {
                case '{':
                    instructions.add(new StringInstruction(currString));
                    currString = "";
                    parseTag(b, "");
                    break;
                default:
                    currString += currChar;
            }
        }

        instructions.add(new StringInstruction(currString));
    }

    private void parseTag(ElixerBuffer buffer, String token) throws IOException {
        char curr = buffer.readChar();
        if(curr != '}') {
            token += curr;
            parseTag(buffer, token);
        } else {
            String split[] = token.split("\\(|\\)");
            if(split.length > 1) {
               parseFunc(split[0], split[1].split(","));
            } else {
                switch (token) {
                    case "L":
                        instructions.add(new LoggerInstruction() {
                            @Override
                            public String getData(ElixerLogger logger, LoggingLevel level, Object... objects) {
                                return level.toString();
                            }
                        });
                        break;
                    case "N":
                        instructions.add(new LoggerInstruction() {
                            @Override
                            public String getData(ElixerLogger logger, LoggingLevel level, Object... objects) {
                                return logger.getNamespace();
                            }
                        });
                        break;
                }
            }
        }
    }

    private void parseFunc(String token, String... args) throws IOException {
        switch (token) {
            case "LST":
                instructions.add(new ListInstruction(args[0]));
                break;
        }
    }

    public String getOut(ElixerLogger logger, LoggingLevel level, Object... objects) {
        String result = "";
        for(LoggerInstruction instruction: instructions) {
            result += instruction.getData(logger, level, objects);
        }
        return result;
    }
}
