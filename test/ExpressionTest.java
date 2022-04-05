import org.junit.Test;
import pt.up.fe.comp.TestUtils;

public class ExpressionTest {
    public void test(String stringToParse, Boolean shouldPass) {
        var parserResult = TestUtils.parse(stringToParse, "Expression");
        if(shouldPass){
            TestUtils.noErrors(parserResult);
        } else {
            TestUtils.mustFail(parserResult);
        }
    }

    /*
    @Test
    public void operations(){
        // Expression,("&&" | "<" | "+" | "-" | "*"| "/"),Expression
        test("true && false", true);
        test("1 < 2", true);
        test("1 +2", true);
        test("1- 2", true);
        test("1*2", true);
        test("1/2", true);
        test("*", false);
        test("true &&", false);
    }

    @Test
    public void accessOperator(){
        // Expression,"[",Expression,"]"
        test("true[true]", true);

        // TODO:
    }

    @Test
    public void length(){
        // Expression,".","length"

        // TODO:
    }

    @Test
    public void memberAccess(){
        // Expression,".",Identifier,"(",[Expression{",",Expres-sion}],")"

        // TODO:
    }

    @Test
    public void terminal(){
        // IntegerLiteral | true | false | Identifier| this
        test("true", true);
        test("false", true);
        test("someIdentifier", true);
        test("this", true);
        test("123", true);

        test("tre", false);
        test("1 2", false);
        test("true false", false);
    }

    @Test
    public void newArray(){
        // "new","int","[",Expression,"]"
        test("new int[123]", true);
        test("new int[true]", true);
        test("new int[]", false);
        test("int[0]", false);
        test("int[]", false);
    }

    @Test
    public void newClass(){
        // "new",Identifier,"(",")"
        test("new Class()", true);
        test("new example()", true);
        test("new Class(something)", false);
        test("new Class", false);
        test("Class()", false);
    }

    @Test
    public void not(){
        // "!",Expression
        test("!1", true);
        test("!true", true);
        test("!", false);
    }

    @Test
    public void parentheses(){
        // "(",Expression,")"
        test("(1 + 1)", true);
        test("(new Class())", true);
        test("()", false);
        test("( )", false);
    }*/
    
}
