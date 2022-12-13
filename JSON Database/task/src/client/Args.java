package client;

import com.beust.jcommander.Parameter;
import com.google.gson.annotations.Expose;


public class Args {

    @Expose
    @Parameter(names = "-t", description = "Type of request")
    private String type;

    @Expose
    @Parameter(names = "-k", description = "Key of the cell")
    private String key;

    @Expose
    @Parameter(names = "-v", description = "Value to save in the database")
    private String value;

    @Parameter(names = "-in", description = "file name of input")
    private String fileName;

    public String getFileName() {
        return this.fileName;
    }
}