import groovy.cli.commons.CliBuilder
import groovy.cli.Option
import groovy.cli.Unparsed
import org.codehaus.groovy.ast.builder.*
import org.codehaus.groovy.ast.expr.*
import org.codehaus.groovy.ast.*

class JenkinsfileShellcheck {

    def filePath

    public @interface Library {
        String[] value();
    }

    static void main(String[] args) {
        def cli = new CliBuilder(usage: 'jenkinsfile-shellcheck [options]')
        cli.i(longOpt:'input', args:1, required:false, argName:'file', defaultValue:'Jenkinsfile', 'input file (default: Jenkinsfile)')
        cli.h(longOpt: 'help', 'usage information')
        cli.footer = """
 shellcheck options can be written after '--'.
 e.g. groovy jenkinsfile-shellcheck.groovy -- -e SC2154
            """
        def through_index = args.findIndexOf { it == '--' }
        def shellcheck_opts = ""
        if( through_index >= 0 ) {
            shellcheck_opts = args[through_index+1..args.size()-1].join(" ")
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
        def jfs = new JenkinsfileShellcheck(filePath:options.i)
        jfs.parse(options.i as File, shellcheck_opts)
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

    void shellcheck(def shell_script_expr, def shellcheck_opts) {
        File.createTempFile("temp",".sh").with {
            write System.getProperty('line.separator') * (shell_script_expr.lineNumber-1)
            append " " * shell_script_expr.columnNumber
            // println shell_script_expr
            append shell_script_expr.text
            def shellcheck_result = "shellcheck -e SC2148 ${shellcheck_opts} ${absolutePath}".execute().text
            if( shellcheck_result?.trim() ) {
                println shellcheck_result.replaceAll(absolutePath, filePath)
            }
        }
    }

    void parse(File jenkinsfile, def shellcheck_opts) {
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
