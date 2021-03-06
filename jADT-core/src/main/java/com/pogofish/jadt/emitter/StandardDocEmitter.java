/*
Copyright 2012 James Iry

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/
package com.pogofish.jadt.emitter;

import java.util.logging.Logger;

import com.pogofish.jadt.Version;
import com.pogofish.jadt.ast.DataType;
import com.pogofish.jadt.ast.Doc;
import com.pogofish.jadt.ast.Imprt;
import com.pogofish.jadt.comments.CommentProcessor;
import com.pogofish.jadt.printer.ASTPrinter;
import com.pogofish.jadt.sink.Sink;
import com.pogofish.jadt.sink.SinkFactory;



public class StandardDocEmitter implements DocEmitter {    
	private static final Logger logger = Logger.getLogger(StandardConstructorEmitter.class.toString());
    private final DataTypeEmitter dataTypeEmitter;
    private final CommentProcessor commentProcessor = new CommentProcessor();
        
    public StandardDocEmitter(DataTypeEmitter dataTypeEmitter) {
        super();
        this.dataTypeEmitter = dataTypeEmitter;
    }

    /* (non-Javadoc)
     * @see sfdc.adt.emitter.Emitter#emit(sfdc.adt.ast.Doc, sfdc.adt.emitter.SinkFactory)
     */
    @Override
    public void emit(SinkFactory factory, Doc doc) {
    	logger.fine("Generating Java source based on " + doc.srcInfo);
        final StringBuilder header = new StringBuilder();
        header.append(ASTPrinter.printComments("", commentProcessor.leftAlign(doc.pkg.comments)));
        header.append(doc.pkg.name.isEmpty() ? "" : ("package " + doc.pkg.name + ";\n\n"));
        if (!doc.imports.isEmpty()) {
            for (Imprt imp : doc.imports) {
                header.append(ASTPrinter.printComments("", commentProcessor.leftAlign(imp.comments)));
                header.append("import " + imp.name + ";\n");
            }
            header.append("\n");
        }
        final String version = new Version().getVersion();
        header.append("/*\nThis file was generated based on " + doc.srcInfo + " using jADT version " + version + " http://jamesiry.github.com/jADT/ . Please do not modify directly.\n\n");
        header.append("The source was parsed as: \n\n");
        
        for (DataType dataType : doc.dataTypes) {
            final Sink sink = factory.createSink(doc.pkg.name.isEmpty() ? dataType.name : doc.pkg.name + "." + dataType.name);
            logger.info("Generating " + sink.getInfo());
            try {
                dataTypeEmitter.emit(sink, dataType, header.toString());
            } finally {
                sink.close();
            }
        }        
    }
}
