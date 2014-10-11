
import java.util.ArrayList;
import java.util.List;

/* 		OO PARSER AND BYTE-CODE GENERATOR FOR TINY PL

Grammar for TinyPL (using EBNF notation) is as follows:

 program ->  decls stmts end
 decls   ->  int idlist ;
 idlist  ->  id { , id } 
 stmts   ->  stmt [ stmts ]
 cmpdstmt->  '{' stmts '}'
 stmt    ->  assign | cond | loop
 assign  ->  id = expr ;
 cond    ->  if '(' rexp ')' cmpdstmt [ else cmpdstmt ]
 loop    ->  while '(' rexp ')' cmpdstmt  
 rexp    ->  expr (< | > | =) expr
 expr    ->  term   [ (+ | -) expr ]
 term    ->  factor [ (* | /) term ]
 factor  ->  int_lit | id | '(' expr ')'

Lexical:   id is a single character; 
	      int_lit is an unsigned integer;
		 equality operator is =, not ==

Sample Program: Factorial

int n, i, f;
n = 4;
i = 1;
f = 1;
while (i < n) {
  i = i + 1;
  f= f * i;
}
end

   Sample Program:  GCD

int x, y;
x = 121;
y = 132;
while (x != y) {
  if (x > y) 
       { x = x - y; }
  else { y = y - x; }
}
end

 */
public class Parser {
	public static void main(String[] args)  {
		System.out.println("Enter program and terminate with 'end'!\n");
		Lexer.lex();
		new Program();
		Code.output();
	}
}

class Program {
	Stmts statements;
	Decls declaration;
	public Program() {
		declaration = new Decls();
		statements = new Stmts();
		if (Lexer.nextToken == Token.KEY_END){
			Code.gen(Code.RETURN_STATEMENT);
		}
	} 
}

class Decls {
	Idlist identifierList;
	public Decls() {
		if(Lexer.nextToken == Token.KEY_INT){
			identifierList = new Idlist();
		}
	}	 	 
}

class Stmts {
	Stmt currStatement;
	Stmts nextStatements;
	public Stmts(){
		currStatement = new Stmt();
		if (Lexer.nextToken == Token.SEMICOLON){
			nextStatements = new Stmts();
		}
	}

}

class Idlist {
	private static List<Character> list = new ArrayList<Character>();
	public Idlist(){
		do {
			Lexer.lex();
			if (Lexer.nextToken == Token.ID){
				list.add(Lexer.ident);
			}
			Lexer.lex();
		} while (Lexer.nextToken == Token.COMMA);
	}

	public static int getIDOf(Character character) {
		return list.indexOf(character);
	}
}

class Stmt {
	Assign assignment;
	Cond ifStatement;
	Loop whileLoop;
	public Stmt (){
		Lexer.lex();
		switch (Lexer.nextToken){
		case Token.ID: 
			assignment = new Assign();
			break;
		case Token.KEY_IF:
			ifStatement = new Cond();
			break;
		case Token.KEY_WHILE:
			whileLoop = new Loop();
			break;
		default:
			break;
		}
	}
} 

class Assign {
	Expr expression;
	public Assign(){
		char ident = Lexer.ident;
		Lexer.lex();
		if (Lexer.nextToken == Token.ASSIGN_OP){
			expression = new Expr();
			Code.iStore(Idlist.getIDOf(ident));
		}
	}
}

class Cond {
	Cmpdstmt cmpdStatements1;
	Rexpr relationalExpr;
	public Cond(){
		Lexer.lex();
		if (Lexer.nextToken == Token.LEFT_PAREN){
			relationalExpr = new Rexpr();
			int ifPointer = Code.addConditionalAndReturnPointer(relationalExpr.getOperator());
			cmpdStatements1 = new Cmpdstmt();
			Lexer.lex();
			if (Lexer.nextToken == Token.KEY_ELSE){
				int gotoPtr = Code.getCodePtr();
				Code.addGoto(false, -1);
				Code.appendToStatement(ifPointer, Code.getCodePtr());
				cmpdStatements1 = new Cmpdstmt();
				Code.appendToStatement(gotoPtr, Code.getCodePtr());
				Lexer.lex();
			}			
		}
	}
}

class Loop {
	Rexpr relationalExpr;
	Cmpdstmt cmpdStatements;
	public Loop(){
		Lexer.lex();
		if (Lexer.nextToken == Token.LEFT_PAREN){
			relationalExpr = new Rexpr();
			int ptr = Code.addConditionalAndReturnPointer(relationalExpr.getOperator());
			cmpdStatements = new Cmpdstmt();
			Code.addGoto(true, ptr);
			Lexer.lex();
		}
	}
}

class Cmpdstmt {
	Stmts statements;
	public Cmpdstmt(){
		Lexer.lex();
		if (Lexer.nextToken == Token.LEFT_BRACE){
			statements = new Stmts();
		}
	}
}

class Rexpr {
	Expr expression;
	private int operator = -1;
	public Rexpr(){
		expression = new Expr();
		if (Lexer.nextToken == Token.LESSER_OP || Lexer.nextToken == Token.GREATER_OP ||
				Lexer.nextToken == Token.ASSIGN_OP || Lexer.nextToken == Token.NOT_EQ){
			this.operator = Lexer.nextToken;
			expression = new Expr();
		}
	}
	
	public int getOperator() {
		return this.operator;
	}
}

class Expr {  
	Term term;
	Expr expr;
	char operator;

	public Expr() {
		term = new Term();
		if (Lexer.nextToken == Token.ADD_OP || Lexer.nextToken == Token.SUB_OP) {
			operator = Lexer.nextChar;
			expr = new Expr();
			Code.addJBOperator(operator);
		}
	}
}

class Term {
	Factor factor;
	Term term;
	char operation;

	public Term() {
		factor = new Factor();
		Lexer.lex();
		if (Lexer.nextToken == Token.MULT_OP || Lexer.nextToken == Token.DIV_OP) {
			operation = Lexer.nextChar;
			term = new Term();
			Code.addJBOperator(operation);
		}
	}
}

class Factor {  
	Expr e;
	int i;

	public Factor() {
		Lexer.lex();
		switch (Lexer.nextToken) {
		case Token.ID:
			Code.iLoad(Idlist.getIDOf(Lexer.ident));
			break;
		case Token.INT_LIT: // number
			Code.iPush(Lexer.intValue);
			break;
		case Token.LEFT_PAREN: // '('
			Lexer.lex();
			e = new Expr();
			Lexer.lex(); // skip over ')'
			break;
		default:
			break;
		}
	}

}

class Code {
	private static String[] code = new String[100];
	private static int codeptr = 0;
	
	public static final String RETURN_STATEMENT = "return";
	private static final String ISTORE = "istore_";
	private static final String ICONST = "iconst_";
	private static final String BIPUSH = "bipush";
	private static final String SIPUSH = "sipush";
	private static final String ILOAD = "iload_";
	private static final String IFCMP = "if_icmp";
	private static final String GOTO = "goto";
	
	public static int getCodePtr() {
		return codeptr;
	}
	
	public static void gen(String genCode) {
		gen(1, genCode);
	}
	
	public static void gen(int increment, String genCode) {
		code[codeptr] = genCode;
		codeptr = codeptr + increment;
	}
	
	public static void appendToStatement(int lineNumber, int value) {
		code[lineNumber] += " " + value;
	}
	
	public static void iStore(int id) {
		gen(ISTORE + id);
	}
	
	public static void iPush(int value) {
		if(value >= 0 && value <= 5) {
			gen(ICONST + value);
		} else if(value >= -128 && value <= 127){
			gen(2, BIPUSH + " " + value);
		} else {
			gen(3, SIPUSH + " " + value);
		}
	}
	
	public static void iLoad(int value) {
		gen(ILOAD + value);
	}
	
	public static void addGoto(boolean isLoop, int iFPointer) {
		if(isLoop) {
			gen(3, GOTO + " " + (iFPointer - 2)); //2 iload statements
			appendToStatement(iFPointer, codeptr);
		} else {
			gen(3, GOTO);
		}
	}
	
	public static int addConditionalAndReturnPointer(int operation) {
		int ptr = codeptr;
		String byteCode = getIFCMPValue(operation);
		gen(3, byteCode);
		return ptr;
	}
	
	private static String getIFCMPValue(int operation) {
		switch (operation) {
		case Token.GREATER_OP:
			return IFCMP + "ge";
		case Token.LESSER_OP:
			return IFCMP + "le";
		case Token.ASSIGN_OP:
			return IFCMP + "ne";
		case Token.NOT_EQ:
			return IFCMP + "eq";
		default:
			break;
		}
		return null;
	}

	public static void addJBOperator(char op) {
		switch(op) {
		case '+' : 
			gen("iadd");
			break;
		case '-':  
			gen("isub");
			break;
		case '*':  
			gen("imul");
			break;
		case '/':  
			gen("idiv");
			break;
		default: 
				break;
		}
	}

	public static void output() {
		for (int i=0; i<codeptr; i++)
			if(isValid(code[i])) {
				System.out.println(i + ": " + code[i]);
			}
	}
	
	public static boolean isValid(String value) {
		return value != null && !"".equals(value.trim()) && !"null".equals(value.trim());
	}
}