/*
 * @(#)ComplexBean.java   
 *
 * Copyright (C) 2008-2013 www.interpss.org
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU LESSER GENERAL PUBLIC LICENSE
 * as published by the Free Software Foundation; either version 2.1
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * @Author Mike Zhou
 * @Version 1.0
 * @Date 01/10/2013
 * 
 *   Revision History
 *   ================
 *
 */

package org.interpss.datamodel.bean.datatype;

import org.interpss.datamodel.bean.BaseJSONBean;
import org.interpss.numeric.util.NumericUtil;

import com.interpss.common.util.IpssLogger;


public class BranchValueBean  implements Comparable<BranchValueBean> {
	public double
		f ,				// value at the from side
		t ;				// value at the to side
	
	public BranchValueBean() { }
	public BranchValueBean(double f, double t) { this.f = f; this.t = t; }
	
	@Override public int compareTo(BranchValueBean bean) {
		int eql = 0;
		
		if (!NumericUtil.equals(this.f, bean.f, BaseJSONBean.CMP_ERR)) {
			IpssLogger.ipssLogger.warning("BranchValueBean.f is not equal"); eql = 1; }
		
		if (!NumericUtil.equals(this.t, bean.t, BaseJSONBean.CMP_ERR)) {
			IpssLogger.ipssLogger.warning("BranchValueBean.t is not equal"); eql = 1; }	
		
		return eql;
	}	
}