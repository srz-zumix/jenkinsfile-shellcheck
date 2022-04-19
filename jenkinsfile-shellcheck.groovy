import groovy.cli.commons.CliBuilder
import groovy.cli.Option
import groovy.cli.Unparsed
import org.codehaus.groovy.ast.builder.*
import org.codehaus.groovy.ast.expr.*
import org.codehaus.groovy.ast.*


class JenkinsfileShellcheck {

    def filePath
    def fileLines
    def noExpandGString

    static void main(String[] args) {
        def cli = new CliBuilder(usage: 'jenkinsfile-shellcheck [options]')
        cli.with {
            i longOpt:'input', args:'+', required:false, argName:'file1,file2,...', valueSeparator:',', defaultValue:'Jenkinsfile', 'input files (default: Jenkinsfile)'
            _ longOpt:'no-expand-gstring', 'do not expand GString values'
            h longOpt: 'help', 'usage information'
        }
        cli.footer = """
 shellcheck options can be written after '--'.
 e.g. groovy jenkinsfile-shellcheck.groovy -- -e SC2154
            """
        def through_index = args.findIndexOf { it == '--' }
        def shellcheck_opts = ""
        if( through_index >= 0 ) {
            if( through_index < args.size()-1 ) {
                shellcheck_opts = args[through_index+1..args.size()-1].join(" ")
            }
            args = args[0..through_index-1]
        }
        def options = cli.parse(args)

        if (!options) {
            cli.usage()
            System.exit(1)
        }
        if (options.help) {
            cli.usage()
            System.exit(0)
        }
        for( input : options.is ) {
            def jfs = new JenkinsfileShellcheck(filePath:input, noExpandGString:options.'no-expand-gstring')
            println(input)
            jfs.parse(input as File, shellcheck_opts)
        }
    }

    static Expression getShellScriptArgument(MethodCallExpression expression) {
        if( expression.arguments instanceof TupleExpression ) {
            for( subExpr : ((TupleExpression)expression.arguments).getExpressions() ) {
                if( subExpr instanceof NamedArgumentListExpression ) {
                    for( e : ((NamedArgumentListExpression)subExpr).getMapEntryExpressions() ) {
                        if( e.keyExpression.text == "script" ) {
                            return e.valueExpression
                        }
                    }
                } else {
                    return subExpr
                }
            }
        }
        return expression.arguments
    }

    String getLine(def lineNumber) {
        return fileLines[lineNumber - 1]
    }

    String getRawSourceCodeTextRange(def expression) {
        if( expression.lineNumber == expression.lastLineNumber ) {
            return getLine(expression.lineNumber).substring(expression.columnNumber - 1, expression.lastColumnNumber - 1)
        }

        def text = getLine(expression.lineNumber).substring(expression.columnNumber - 1)
        for( def i = expression.lineNumber + 1; i < expression.lastLineNumber; ++i ) {
            text += '\n'
            text += getLine(i)
        }
        text += '\n'
        text += getLine(expression.lastLineNumber).substring(0, expression.lastColumnNumber - 1)
        return text
    }

    static replaceStartEnd(String text, def len) {
        return " " * len + text.substring(len, text.size() - len)
    }

    String getRawSourceCodeText(def expression) {
        def toText = {
            def text = getRawSourceCodeTextRange(expression)
            if( expression instanceof ConstantExpression ) {
                if( text.startsWith("'''") ) {
                    return replaceStartEnd(text, 3)
                }
                return replaceStartEnd(text, 1)
            } else if( expression instanceof GStringExpression ) {
                // if( expression.isConstantString() ) {
                //     return getRawSourceCodeText(expression.asConstantString())
                // }
                if( text.startsWith('"""') ) {
                    return replaceStartEnd(text, 3)
                }
                if( text.startsWith('$/') ) {
                    return replaceStartEnd(text, 2)
                }
                return replaceStartEnd(text, 1)
            }
            return text
        }
        return " " * expression.columnNumber + toText()
    }

    String getSourceCodeText(def expression) {
        // return getRawSourceCodeText(expression)
        return " " * expression.columnNumber + expression.text
    }

    static List getVariables(def expression) {
        def vars = []
        if( expression instanceof GStringExpression ) {
            for( e : expression.getValues() ) {
                if( e instanceof VariableExpression ) {
                    vars << e.text
                } else if( e instanceof PropertyExpression ) {
                    vars << e.text
                } else {
                }
            }
        }
        return vars
    }

    String replaceGStringVariables(String text, def expression) {
        if( expression instanceof GStringExpression && !this.noExpandGString ) {
            for( e : expression.getValues() ) {
                def var = e.text
                if( e instanceof VariableExpression ) {
                    text = text.replaceAll('[$]' + var, "${var}")
                } else if( e instanceof PropertyExpression ) {
                    // if( e.objectExpression.text == 'env' ) {
                    //     text = text.replaceAll('[$]\\{' + var + '\\}', "\\\${${e.property.text}}")
                    // } else {
                        text = text.replaceAll('[$]\\{' + var + '\\}', "${var}")
                    // }
                }
            }
        }
        return text
    }

    void shellcheck(def shell_script_expr, def shellcheck_opts) {
        File.createTempFile("temp",".sh").with {
            write System.getProperty('line.separator') * (shell_script_expr.lineNumber-1)
            def codeText = getSourceCodeText(shell_script_expr)
            codeText = replaceGStringVariables(codeText, shell_script_expr)
            // println codeText
            append codeText
            def shellcheck_result = "shellcheck -e SC2148 ${shellcheck_opts} ${absolutePath}".execute().text
            if( shellcheck_result?.trim() ) {
                println shellcheck_result.replaceAll(absolutePath, filePath)
            }
        }
    }

    void parse(File jenkinsfile, def shellcheck_opts) {
        fileLines = jenkinsfile.readLines()
        List<ASTNode> astNodes = new AstBuilder().buildFromString(jenkinsfile.text);
        GroovyCodeVisitor visitor = new CodeVisitorSupport() {
            @Override
            public void visitMethodCallExpression(MethodCallExpression expression) {
                super.visitMethodCallExpression(expression)
                if( expression.method.text == "sh" ) {
                    // println "${jenkinsfile.name}:${expression.lineNumber}: ${expression.text}"
                    def shell_script_expr = getShellScriptArgument(expression)
                    shellcheck(shell_script_expr, shellcheck_opts)
                }
            }
        }
        for (ASTNode node : astNodes) {
            node.visit(visitor);
        }
    }
}

@interface Library {
    String[] value()
}
