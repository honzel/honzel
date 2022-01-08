package com.honzel.test;

import com.honzel.core.util.resolver.DefaultResolver;
import com.honzel.core.util.resolver.Resolver;

public class ResovlerTester {
	

	public void testStartsWithResolver() {
		String input=",asdfa\\\\\\ ,\\\\\\\\\\\\ ,\\\\\\\\\\, \\c\\d\\ ,\\\\\\ bs \\\\\\ ,\\bc\\d";
		Resolver resolver=new DefaultResolver(",", "").reset(input).useTrim(true);
		System.out.println("------------------------------------------------------------------");
		while (resolver.hasNext()) {
			System.out.println("'" + resolver.next(false) + "'");
			System.out.println("'" + resolver.next() + "'");
			String prefix = resolver.next();
			System.out.println("------------------------------------------------------------------");
		}
	}
	
	public void testExpressionResolver() {
		StringBuilder input= new StringBuilder(".aaaa  [  bb  ] \\cc\\       [  30].ff(kk). pp, pp挂起\\ ,");
		String[] types={"索引值","key值","嵌套", "分隔"};
		Resolver resolver=new DefaultResolver("[(.,", "])").reset(input).useTrim(true);
//		resolver.useTypes(Resolver.ALL_OF_TYPES ^ (1 << 2));
//		int noken = 2;
//		resolver.useTypes(1 << noken);
		System.out.println("------------------------------------------------------------------");
		
		while (resolver.hasNext()) {
			System.out.println(resolver);
			System.out.println("'"+resolver.next(true, true, true)+"'");
			System.out.println("'"+resolver.next()+"'");
			String type="";
			switch (resolver.getType()) {
			case Resolver.START:
				type="开始";
				break;
			case Resolver.END:
				type="错误";
				break;
			case Resolver.LINK:
				type = "不确定类型";
				break;
			default:
				type = String.valueOf(resolver.getType());
				break;
			}
			System.out.println(type);
			System.out.println(resolver.getStart()+"-"+resolver.getEnd());
			System.out.println("'"+resolver.getInput().subSequence(resolver.getStart(), resolver.getEnd())+"'");
			System.out.println("'"+resolver.nextInt()+"'");
			System.out.println("------------------------------------------------------------------");
			
//			resolver.useType(1);
		}
		
//		resolver.reset(resolver.getStart() + 3).hasNext();
		System.out.println("'" + resolver.next(false, true, true) + "'");
		System.out.println(resolver.getStart());
		resolver.hasNext();
		System.out.println("'" + resolver.next(false, true, true) + "'");
		System.out.println(resolver.getStart());
		resolver.hasNext();
		System.out.println("'" + resolver.next(false, true, true) + "'");
		System.out.println(resolver.getStart());
		System.out.println("------------------------------------------------------------------");
		input.append(".aaaa.bbbb.ccccc[d][]asdf asd [ pppp \\pp");
		System.out.println(input);
		while (resolver.hasNext()) {
			System.out.println(resolver);
			System.out.println(resolver.next(false, true, true));
			System.out.println("'"+resolver.next()+"'");
			System.out.println(resolver.getStart()+"-"+resolver.getEnd());
			System.out.println("'"+resolver.getInput().subSequence(resolver.getStart(), resolver.getEnd())+"'");
			System.out.println("'"+resolver.nextInt()+"'");
			System.out.println("------------------------------------------------------------------");
		}
	}
	
	public void testMoveBit() {
		// TODO Auto-generated method stub
		int result = 0;
		for(int i = - 4; i < 31; i ++ ) {
			result = 1 << i;
			System.out.println("移位" + i + ": " + result);
			System.out.println(1 << i);
			System.out.println("----------------------------------------------------------");
		}
	}
	
	public void testIsInTypes() {
		String src = "aaa/bbb/ccc/ddd";
		System.out.println(src.lastIndexOf("aaa", 0));
	}
	
	public static void main(String[] args) {
		ResovlerTester tester = new ResovlerTester();
//		tester.testMoveBit();
//		tester.testStartsWithResolver();
		tester.testExpressionResolver();
//		tester.testIsInTypes();
	}
}
