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
package com.pogofish.jadt.comments;

import static com.pogofish.jadt.ast.BlockToken._BlockWhiteSpace;
import static com.pogofish.jadt.ast.BlockToken._BlockWord;
import static com.pogofish.jadt.ast.JDToken._JDWhiteSpace;
import static com.pogofish.jadt.ast.JavaComment._JavaBlockComment;
import static com.pogofish.jadt.ast.JavaComment._JavaDocComment;
import static com.pogofish.jadt.ast.JavaComment._JavaEOLComment;
import static com.pogofish.jadt.util.Util.list;
import static com.pogofish.jadt.util.Util.set;
import static org.junit.Assert.assertEquals;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.junit.Test;

import com.pogofish.jadt.ast.JDTagSection;
import com.pogofish.jadt.ast.JDToken;
import com.pogofish.jadt.ast.JavaComment;
import com.pogofish.jadt.printer.ASTPrinter;
import com.pogofish.jadt.util.Util;

public class CommentProcessorTest {
    private static final JDToken ONEWS = _JDWhiteSpace(" ");
    private static final List<JDTagSection> NO_TAG_SECTIONS = Util.<JDTagSection>list();
    
    @Test
    public void testStripTags() {
        testStripTags("/** */\n", list("/** */"), set("@foo"));
        testStripTags("/** @bar whatever */\n", list("/** @bar whatever */"), set("@foo"));
        testStripTags("/** */\n", list("/** @foo whatever */"), set("@foo"));
        testStripTags("/** blah blah @hello there\n */\n", list("/** blah blah @hello there\n * @foo first \n @foo second *\n*/"), set("@foo"));
        
        // make sure strip tags leaves other comment types alone
        final CommentProcessor commentProcessor = new CommentProcessor();
        @SuppressWarnings("unchecked")
        List<JavaComment> comments = list(_JavaEOLComment("//whatever"), _JavaBlockComment(list(list(_BlockWord("/*"), _BlockWhiteSpace(" "), _BlockWord("*/")))));
        assertEquals(comments, commentProcessor.stripTags(set("@foo"), comments));
    }
    
    private void testStripTags(String expected, List<String> inputs, Set<String> tags) {
        final JavaDocParser parser = new JavaDocParser();
        final List<JavaComment> outputs = new ArrayList<JavaComment>(inputs.size());
        final CommentProcessor commentProcessor = new CommentProcessor();
        for (String input : inputs) {
            final JavaComment comment = parser.parse(new StringReader(input));
            outputs.add(comment);
        }
        final List<JavaComment> stripped = commentProcessor.stripTags(tags, outputs);
        final String actual = ASTPrinter.printComments("", stripped);
        assertEquals(expected, actual);    
   }
    
    @Test
    public void testJavaDocOnly() {
        final CommentProcessor commentProcessor = new CommentProcessor();
        JavaComment javaDocComment = _JavaDocComment("/**", list(ONEWS), NO_TAG_SECTIONS, "*/");
        @SuppressWarnings("unchecked")
        List<JavaComment> comments = list(javaDocComment, _JavaEOLComment("//whatever"), _JavaBlockComment(list(list(_BlockWord("/*"), _BlockWhiteSpace(" "), _BlockWord("*/")))));
        assertEquals(list(javaDocComment), commentProcessor.javaDocOnly(comments));
    }

    @Test
    public void testLeftAlignBlock() {
        testBlockAlign("/* */\n", list("/* */"));
        testBlockAlign("/*\n\n */\n", list("/*\n\n */"));
        testBlockAlign("/* */\n/* hello */\n", list("/* */", "/* hello */"));
        testBlockAlign("/* \n */\n", list("/* \n*/"));
        testBlockAlign("/* hello\n * goodbye *\n */\n", list("/* hello\n    * goodbye *\n*/"));
        testBlockAlign("/* hello\n     goodbye *\n */\n", list("/* hello\n     goodbye *\n*/"));
    }

    private void testBlockAlign(String expected, List<String> inputs) {
        final BlockCommentParser parser = new BlockCommentParser();
        final List<JavaComment> outputs = new ArrayList<JavaComment>(inputs.size());
        final CommentProcessor commentProcessor = new CommentProcessor();
        for (String input : inputs) {
            final JavaComment comment = parser.parse(new StringReader(input));
            outputs.add(comment);
        }
        final List<JavaComment> aligned = commentProcessor.leftAlign(outputs);
        final String actual = ASTPrinter.printComments("", aligned);
        assertEquals(expected, actual);
    }
    
    @Test
    public void testLeftAlignJavaDoc() {
        testJavaDocAlign("/** */\n", list("/** */"));
        testJavaDocAlign("/**\n\n */\n", list("/**\n\n */"));
        testJavaDocAlign("/** */\n/** hello */\n", list("/** */", "/** hello */"));
        testJavaDocAlign("/** \n */\n", list("/** \n*/"));
        testJavaDocAlign("/** hello\n * goodbye *\n */\n", list("/** hello\n    * goodbye *\n*/"));
        testJavaDocAlign("/** hello\n     goodbye *\n */\n", list("/** hello\n     goodbye *\n*/"));
        testJavaDocAlign("/** hello\n * @goodbye *\n */\n", list("/** hello\n    * @goodbye *\n*/"));
        testJavaDocAlign("/** hello\n * @goodbye *\n fdasdf */\n", list("/** hello\n    * @goodbye *\n fdasdf */"));
        testJavaDocAlign("/** hello\n     @goodbye you\n @goodbye\n *\n */\n", list("/** hello\n     @goodbye you\n @goodbye\n *\n*/"));
    }

    private void testJavaDocAlign(String expected, List<String> inputs) {
        final JavaDocParser parser = new JavaDocParser();
        final List<JavaComment> outputs = new ArrayList<JavaComment>(inputs.size());
        final CommentProcessor commentProcessor = new CommentProcessor();
        for (String input : inputs) {
            final JavaComment comment = parser.parse(new StringReader(input));
            outputs.add(comment);
        }
        final List<JavaComment> aligned = commentProcessor.leftAlign(outputs);
        final String actual = ASTPrinter.printComments("", aligned);
        assertEquals(expected, actual);
    }
    
    @Test
    public void testParamDoc() {
        testParamDoc("", "foo", list("/** */"));
        testParamDoc("", "foo", list("/** @flurg foo */"));
        testParamDoc("", "foo", list("/** @param @foo * */"));
        testParamDoc("/**\n * hello @world */\n", "foo", list("/** @param foo hello @world */"));
        testParamDoc("/**\n * hello\n */\n", "foo", list("/** \n * @param foo hello\n */"));
        testParamDoc("/**\n * hello *\n */\n", "foo", list("/** \n * @param bar hello1\n * @param foo hello *\n * @param baz hello2\n */"));
    }

    private void testParamDoc(String expected, String paramName, List<String> inputs) {
        final JavaDocParser parser = new JavaDocParser();
        final List<JavaComment> outputs = new ArrayList<JavaComment>(inputs.size());
        final CommentProcessor commentProcessor = new CommentProcessor();
        for (String input : inputs) {
            final JavaComment comment = parser.parse(new StringReader(input));
            outputs.add(comment);
        }
        final List<JavaComment> paramDoc = commentProcessor.paramDoc(paramName, outputs);
        final String actual = ASTPrinter.printComments("", paramDoc);
        assertEquals(expected, actual);
     }
    

    @Test
    public void testParamDocNonJavaDoc() {
        final CommentProcessor commentProcessor = new CommentProcessor();
        @SuppressWarnings("unchecked")
        List<JavaComment> comments = list(_JavaEOLComment("//whatever"), _JavaBlockComment(list(list(_BlockWord("/*"), _BlockWhiteSpace(" "), _BlockWord("*/")))));
        assertEquals(Util.<JavaComment>list(), commentProcessor.paramDoc("foo", comments));
    }
    
}
