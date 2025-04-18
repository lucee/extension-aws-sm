package org.lucee.extension.aws.sm.util;

import lucee.loader.engine.CFMLEngine;
import lucee.loader.engine.CFMLEngineFactory;
import lucee.runtime.PageContext;
import lucee.runtime.exp.PageException;
import lucee.runtime.ext.function.BIF;
import lucee.runtime.util.Cast;

public class Functions {

	public static final String SERIALIZE_CLASS = "lucee.runtime.functions.dynamicEvaluation.Serialize";
	public static final String EVALUATE_CLASS = "lucee.runtime.functions.dynamicEvaluation.Evaluate";

	public static String serialize(Object var) throws PageException {
		return serialize(pc(), var);
	}

	public static String serialize(PageContext pc, Object var) throws PageException {
		CFMLEngine engine = CFMLEngineFactory.getInstance();
		Cast caster = engine.getCastUtil();
		try {
			BIF bif = engine.getClassUtil().loadBIF(pc, SERIALIZE_CLASS);
			return caster.toString(bif.invoke(pc, new Object[] { var }));
		} catch (Exception e) {
			throw caster.toPageException(e);
		}
	}

	private static PageContext pc() {
		CFMLEngine engine = CFMLEngineFactory.getInstance();
		return engine.getThreadPageContext();
	}

	public static Object evaluate(Object obj) throws PageException {
		return evaluate(pc(), obj);
	}

	public static Object evaluate(PageContext pc, Object obj) throws PageException {
		CFMLEngine engine = CFMLEngineFactory.getInstance();
		Cast caster = engine.getCastUtil();
		try {
			BIF bif = engine.getClassUtil().loadBIF(pc, EVALUATE_CLASS);
			return bif.invoke(pc, new Object[] { new Object[] { obj } });
		} catch (Exception e) {
			throw caster.toPageException(e);
		}
	}
}
