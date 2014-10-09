package edu.buffalo.fpl;

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
		Program program = new Program();
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
			//TODO Check this
			Lexer.lex();		
		}
	} 
}

class Decls {
	Idlist identifierList;
	public Decls() {
		if(Lexer.nextToken == Token.KEY_INT){
			identifierList = new Idlist();
			Lexer.lex();
		}
	}	 	 
}

class Stmts {
	Stmt statement;
	Stmts statements1;
	public Stmts(){
		statement = new Stmt();
		if (Lexer.nextToken == Token.SEMICOLON){
			Lexer.lex();
			statements1 = new Stmts();
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
		switch (Lexer.nextToken){
		case Token.ID: 
			Lexer.lex();
			assignment = new Assign();
			break;
		case Token.KEY_IF:
			Lexer.lex();
			ifStatement = new Cond();
			break;
		case Token.KEY_WHILE:
			Lexer.lex();
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
		if (Lexer.nextToken == Token.ASSIGN_OP){
			Lexer.lex();
			expression = new Expr();
		}
	}
}

class Cond {
	Cmpdstmt cmpdStatements1;
	Rexpr relationalExpr;
	public Cond(){
		if (Lexer.nextToken == Token.LEFT_PAREN){
			Lexer.lex();
			relationalExpr = new Rexpr();
			Lexer.lex();
			cmpdStatements1 = new Cmpdstmt();
			if (Lexer.nextToken == Token.KEY_ELSE){
				cmpdStatements1 = new Cmpdstmt();
			}			
		}
	}
}

class Loop {
	Rexpr relationalExpr;
	Cmpdstmt cmpdStatements;
	public Loop(){
		if (Lexer.nextToken == Token.LEFT_PAREN){
			Lexer.lex();
			relationalExpr = new Rexpr();
			Lexer.lex();
			cmpdStatements = new Cmpdstmt();
		}		
	}
}

class Cmpdstmt {
	Stmts statements;
	public Cmpdstmt(){
		if (Lexer.nextToken == Token.LEFT_BRACE){
			Lexer.lex();
			statements = new Stmts();
			Lexer.lex();
		}
	}
}

class Rexpr {
	Expr expression;
	public Rexpr(){
		expression = new Expr();
		if (Lexer.nextToken == Token.LESSER_OP || Lexer.nextToken == Token.GREATER_OP ||
			Lexer.nextToken == Token.ASSIGN_OP || Lexer.nextToken == Token.NOT_EQ){
			Lexer.lex();
			expression = new Expr();
		}
	}
}

class Expr {  
	Term t;
	Expr e;
	char op;

	public Expr() {
		t = new Term();
		if (Lexer.nextToken == Token.ADD_OP || Lexer.nextToken == Token.SUB_OP) {
			op = Lexer.nextChar;
			Lexer.lex();
			e = new Expr();
			Code.gen(Code.opcode(op));
		}
	}
}

class Term {  
	Factor f;
	Term t;
	char op;

	public Term() {
		f = new Factor();
		if (Lexer.nextToken == Token.MULT_OP || Lexer.nextToken == Token.DIV_OP) {
			op = Lexer.nextChar;
			Lexer.lex();
			t = new Term();
			Code.gen(Code.opcode(op));
		}
	}
}

class Factor {  
	Expr e;
	int i;

	public Factor() {
		switch (Lexer.nextToken) {
		case Token.INT_LIT: // number
			i = Lexer.intValue;
			System.out.println(i);
			Lexer.lex();
			Code.gen("iconst_" + i);
			break;
		case Token.ID:
			Lexer.lex();
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
	static String[] code = new String[100];
	static int codeptr = 0;
	
	public static void gen(String s) {
		code[codeptr] = s;
		codeptr++;
	}
	
	public static String opcode(char op) {
		switch(op) {
		case '+' : return "iadd";
		case '-':  return "isub";
		case '*':  return "imul";
		case '/':  return "idiv";
		default: return "";
		}
	}
	
	public static void output() {
		for (int i=0; i<codeptr; i++)
			System.out.println(i + ": " + code[i]);
	}
}
