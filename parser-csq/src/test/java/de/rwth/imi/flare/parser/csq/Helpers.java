package de.rwth.imi.flare.parser.csq;

import java.io.InputStream;
import java.util.Scanner;

public class Helpers {
    public String readResourceIntoString(String resourcePath){
        InputStream resourceAsStream = this.getClass().getClassLoader().getResourceAsStream(resourcePath);
        if(resourceAsStream == null){
            return null;
        }
        Scanner resourceScanner = new Scanner(resourceAsStream);

        StringBuilder builder = new StringBuilder();
        builder.append(resourceScanner.nextLine());
        while (resourceScanner.hasNextLine()){
            builder.append("\n");
            builder.append(resourceScanner.nextLine());
        }
        return builder.toString();
    }
}
