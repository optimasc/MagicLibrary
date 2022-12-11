package com.willcode4beer.infix;

import java.util.Stack;

/**
 * Class to evaluate infix and postfix expressions.
 *
 * @author Paul E. Davis (feedback@willcode4beer.com)
 */
public class InfixPostfixEvaluator
{

    /**
     * Operators in reverse order of precedence.
     */
    private static final String operators = "-+/*";
    private static final String operands = "0123456789";

    public static int evalInfixAsInt(String infix)
    {
        return evaluatePostfixAsInt(convert2Postfix(infix));
    }


    public static float evalInfixAsFloat(String infix)
    {
        return evaluatePostfixAsFloat(convert2Postfix(infix));
    }
    
    private static String convert2Postfix(String infixExpr)
    {
        char[] chars = infixExpr.toCharArray();
        char c;
        int i = 0;
        Stack stack = new Stack();
        StringBuffer out = new StringBuffer(infixExpr.length());

        while (i < chars.length)
        {
            c = chars[i];
            if (isOperator(c))
            {
                while (!stack.isEmpty() && ((Character) stack.peek()).charValue() != '(')
                {
                    if (operatorGreaterOrEqual(((Character) stack.peek()).charValue(), c))
                    {
                        out.append(stack.pop());
                    } else
                    {
                        break;
                    }
                }
                stack.push(new Character(c));
            } else if (c == '(')
            {
                stack.push(new Character(c));
            } else if (c == ')')
            {
                while (!stack.isEmpty() && ((Character) stack.peek()).charValue() != '(')
                {
                    out.append(stack.pop());
                }
                if (!stack.isEmpty())
                {
                    stack.pop();
                }
            } else if (isOperand(c))
            {
                while ((i < chars.length) && (isOperand(chars[i])))
                {
                    c = chars[i];
                    out.append(c);
                    i++;
                }
                out.append(' ');
                continue;
            } else
            {
                throw new IllegalArgumentException("Invalid expression character:"+c);
            }
            i++;
        }
        while (!stack.empty())
        {
            out.append(stack.pop());
        }
        return out.toString();
    }

    private static int evaluatePostfixAsInt(String postfixExpr)
    {
        char[] chars = postfixExpr.toCharArray();
        int i;
        char c;
        Stack stack = new Stack();
        i = 0;
        while (i < chars.length)
        {
            c = chars[i];
            if (isOperand(c))
            {
                String s = "";
                while ((i < chars.length) && (isOperand(chars[i])))
                {
                    s += chars[i];
                    i++;
                }
                stack.push(new Integer(Integer.parseInt(s))); // convert char to int val
                continue;
            } else if (isOperator(c))
            {
                int op1 = ((Integer) stack.pop()).intValue();
                int op2 = ((Integer) stack.pop()).intValue();
                int result;
                switch (c)
                {
                    case '*':
                        result = op1 * op2;
                        stack.push(new Integer(result));
                        break;
                    case '/':
                        result = op2 / op1;
                        stack.push(new Integer(result));
                        break;
                    case '+':
                        result = op1 + op2;
                        stack.push(new Integer(result));
                        break;
                    case '-':
                        result = op2 - op1;
                        stack.push(new Integer(result));
                        break;
                }
            }
            i++; // Next character;
        }
        return ((Integer) stack.pop()).intValue();
    }

    private static float evaluatePostfixAsFloat(String postfixExpr)
    {
        char[] chars = postfixExpr.toCharArray();
        int i;
        char c;
        Stack stack = new Stack();
        i = 0;
        while (i < chars.length)
        {
            c = chars[i];
            if (isOperand(c))
            {
                String s = "";
                while ((i < chars.length) && (isOperand(chars[i])))
                {
                    s += chars[i];
                    i++;
                }
                stack.push(Float.valueOf(s)); // convert char to int val
                continue;
            } else if (isOperator(c))
            {
                float op1 = ((Float) stack.pop()).floatValue();
                float op2 = ((Float) stack.pop()).floatValue();
                float result;
                switch (c)
                {
                    case '*':
                        result = op1 * op2;
                        stack.push(new Float(result));
                        break;
                    case '/':
                        result = op2 / op1;
                        stack.push(new Float(result));
                        break;
                    case '+':
                        result = op1 + op2;
                        stack.push(new Float(result));
                        break;
                    case '-':
                        result = op2 - op1;
                        stack.push(new Float(result));
                        break;
                }
            }
            i++; // Next character;
        }
        return ((Float)stack.pop()).floatValue();
    }



    private static int getPrecedence(char operator)
    {
        int ret = 0;
        if (operator == '-' || operator == '+')
        {
            ret = 1;
        } else if (operator == '*' || operator == '/')
        {
            ret = 2;
        }
        return ret;
    }

    private static boolean operatorGreaterOrEqual(char op1, char op2)
    {
        return getPrecedence(op1) >= getPrecedence(op2);
    }

    private static boolean isOperator(char val)
    {
        return operators.indexOf(val) >= 0;
    }

    private static boolean isOperand(char val)
    {
        return operands.indexOf(val) >= 0;
    }
}
