JADT generates [Java source files](how_adt.html) from [algebraic datatype](what_adt.html) description [files](syntax.html. The resulting Java is easy to [use](how_adt.html).

JADT uses very liberal [licensing](licensing.html).

Sample
======

Here's a sample JADT file that describes the abstract syntax tree for a fragment of a fictional language

    package pogofish.jadt.sampleast
    
    import java.util.List
    
    Type =
         Int
       | Long
   
    Function = Function(Type returnType, String name, List<Arg> args, List<Statement> statements)
    
    Arg = Arg(Type type, String name)
    
    Statement =
        Declaration(Type type, String name, Expression expression)
      | Assignment(String name, Expression expression)
      | Return(Expression expression)
    
    Expression =
        Add(Expression left, Expression right)
      | Variable(String name)
      | Literal(int value)


Usage
=====
To generate Java from the command line use

    java -cp "path to JADT-core" pogofish.JADT "full path of input file" "base directory for output"

To generate using ant create a build.xml along these lines
   
    <?xml version="1.0"?>

    <project name="JADTTaskExample" default="compile" basedir=".">
      <taskdef name="jadt" classname="pogofish.jadt.ant.JADTAntTask"/>

      <target name="compile" depeneds="generate">
        <!-- normal compile stuff -->
      </target>
      
      <target name="generate">
        <jadt srcFile="full path of input file" destDir = "base directory for Java output"/>
      </target>
    </project>
    
And then run ant, telling it where to find JADT-core and JADT-ant
    
    ant -lib "directory that has both JADT-core and JADT-ant"

    
To use the generated Java, you'll need some imports

    import static pogofish.jadt.sampleast.Arg.*;
    import static pogofish.jadt.sampleast.Expression.*;
    import static pogofish.jadt.sampleast.Function.*;
    import static pogofish.jadt.sampleast.Statement.*;
    import static pogofish.jadt.sampleast.Type.*;

    import java.util.*;

    import pogofish.jadt.sampleast.Expression.*;
    import pogofish.jadt.sampleast.Statement.*; 

Here's an example of creating a complete function using generated factory methods

    public Function sampleFunction() {   
           return _Function(_Int, "addTwo", list(_Arg(_Int, "x"), _Arg(_Int, "y")), list(
                   _Return(_Add(_Variable("x"), _Variable("y")))
                   ));
    }

    public static <A> List<A> list(A... elements) {
        final List<A> list = new ArrayList<A>(elements.length);
        for (A element : elements) {
            list.add(element);
        }
        return list;
    }    

Here's a sample function that returns all the integer literals in an expression

    public Set<Integer> expressionLiterals(Expression expression) {
        return expression.accept(new Expression.Visitor<Set<Integer>>() {
            @Override
            public Set<Integer> visit(Add x) {
                final Set<Integer> results = expressionLiterals(x.left);
                results.addAll(expressionLiterals(x.right));
                return results;
            }

            @Override
            public Set<Integer> visit(Variable x) {
                return Collections.<Integer>emptySet();
            }

            @Override
            public Set<Integer> visit(Literal x) {
                return Collections.singleton(x.value);
            }
        });
    }
     
And here's a sample function that returns true only if a list of statements has a return statement.  Unlike the previous example, this one uses a VisitorWithDefault so that only relevant cases need to be considered.

    public boolean hasReturn(List<Statement> statements) {
        boolean hasReturn = false;
        for (Statement statement : statements) {
            hasReturn = hasReturn || statement.accept(new Statement.VisitorWithDefault<Boolean>() {                
                @Override
                public Boolean visit(Return x) {
                    return true;
                }
                 
                @Override
                public Boolean getDefault(Statement x) {
                    return false;
                }});
        }
        return hasReturn;
    }
