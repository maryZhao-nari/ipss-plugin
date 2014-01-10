/*
 * @(#)AAclfBusDataHelper.java   
 *
 * Copyright (C) 2008 www.interpss.org
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
 * @Date 02/01/2011
 * 
 *   Revision History
 *   ================
 *
 */

package org.interpss.mapper.odm.impl.aclf;

import static com.interpss.common.util.IpssLogger.ipssLogger;
import static org.interpss.mapper.odm.ODMFunction.BusXmlRef2BusId;
import static org.interpss.mapper.odm.ODMUnitHelper.ToActivePowerUnit;
import static org.interpss.mapper.odm.ODMUnitHelper.ToAngleUnit;
import static org.interpss.mapper.odm.ODMUnitHelper.ToApparentPowerUnit;
import static org.interpss.mapper.odm.ODMUnitHelper.ToReactivePowerUnit;
import static org.interpss.mapper.odm.ODMUnitHelper.ToVoltageUnit;
import static org.interpss.mapper.odm.ODMUnitHelper.ToYUnit;

import javax.xml.bind.JAXBElement;

import org.apache.commons.math3.complex.Complex;
import org.ieee.odm.model.aclf.AclfParserHelper;
import org.ieee.odm.schema.AngleXmlType;
import org.ieee.odm.schema.ApparentPowerUnitType;
import org.ieee.odm.schema.BusGenDataXmlType;
import org.ieee.odm.schema.BusLoadDataXmlType;
import org.ieee.odm.schema.LFGenCodeEnumType;
import org.ieee.odm.schema.LFLoadCodeEnumType;
import org.ieee.odm.schema.LoadflowBusXmlType;
import org.ieee.odm.schema.LoadflowGenDataXmlType;
import org.ieee.odm.schema.LoadflowLoadDataXmlType;
import org.ieee.odm.schema.PowerXmlType;
import org.ieee.odm.schema.ReactivePowerUnitType;
import org.ieee.odm.schema.ReactivePowerXmlType;
import org.ieee.odm.schema.ShuntCompensatorBlockXmlType;
import org.ieee.odm.schema.ShuntCompensatorModeEnumType;
import org.ieee.odm.schema.ShuntCompensatorXmlType;
import org.ieee.odm.schema.VoltageXmlType;
import org.ieee.odm.schema.YXmlType;
import org.interpss.numeric.datatype.LimitType;
import org.interpss.numeric.datatype.Unit.UnitType;

import com.interpss.CoreObjectFactory;
import com.interpss.DStabObjectFactory;
import com.interpss.common.datatype.UnitHelper;
import com.interpss.common.exp.InterpssException;
import com.interpss.core.aclf.AclfBus;
import com.interpss.core.aclf.AclfGen;
import com.interpss.core.aclf.AclfGenCode;
import com.interpss.core.aclf.AclfLoad;
import com.interpss.core.aclf.AclfLoadCode;
import com.interpss.core.aclf.BaseAclfNetwork;
import com.interpss.core.aclf.adj.FunctionLoad;
import com.interpss.core.aclf.adj.PQBusLimit;
import com.interpss.core.aclf.adj.PVBusLimit;
import com.interpss.core.aclf.adj.QBank;
import com.interpss.core.aclf.adj.RemoteQBus;
import com.interpss.core.aclf.adj.RemoteQControlType;
import com.interpss.core.aclf.adj.SwitchedShunt;
import com.interpss.core.aclf.adj.VarCompensatorControlMode;
import com.interpss.core.aclf.adpter.AclfLoadBusAdapter;
import com.interpss.core.aclf.adpter.AclfPQGenBus;
import com.interpss.core.aclf.adpter.AclfPVGenBus;
import com.interpss.core.aclf.adpter.AclfSwingBus;
import com.interpss.core.acsc.AcscBus;
import com.interpss.dstab.DStabBus;

/**
 * Aclf bus data ODM mapping helper functions
 * 
 * @author mzhou
 *
 */
public class AclfBusDataHelper<TBus extends AclfBus> {
	private BaseAclfNetwork<?,?> aclfNet = null;
	private TBus bus = null;
	
	/**
	 * constructor
	 * 
	 * @param aclfNet
	 */
	public AclfBusDataHelper(BaseAclfNetwork<?,?> aclfNet) {
		this.aclfNet = aclfNet;
	}
	
	/**
	 * set AclfBus object
	 * 
	 * @param bus
	 */
	public void setBus(TBus bus) {
		this.bus = bus;
	}
	
	/**
	 * map the Loadflow bus ODM object info to the AclfBus object
	 * 
	 * @param xmlBusData
	 * @throws InterpssException
	 */
	public void setAclfBusData(LoadflowBusXmlType xmlBusData) throws InterpssException {
		VoltageXmlType vXml = xmlBusData.getVoltage();
		double vpu = 1.0;
		if (vXml != null) {
			UnitType unit = ToVoltageUnit.f(vXml.getUnit());
			vpu = UnitHelper.vConversion(vXml.getValue(), bus.getBaseVoltage(), unit, UnitType.PU);
		}
		double angRad = 0.0;
		if (xmlBusData.getAngle() !=  null) {
			UnitType unit = ToAngleUnit.f(xmlBusData.getAngle().getUnit()); 
			angRad = UnitHelper.angleConversion(xmlBusData.getAngle().getValue(), unit, UnitType.Rad); 
		}
		bus.setVoltage(vpu, angRad);
		//System.out.println(aclfBus.getId() + "  " + Number2String.toStr(aclfBus.getVoltage()));

		if (xmlBusData.getGenData()!=null) {
			mapGenData(xmlBusData.getGenData());
			/* there is no need to do the check. the mapGenData() method should do the job
			if(xmlBusData.getGenData().getEquivGen().getValue().getCode()!=LFGenCodeEnumType.NONE_GEN)
			mapGenData(xmlBusData.getGenData());
			else 
				aclfBus.setGenCode(AclfGenCode.NON_GEN);
			*/
		} else {
			bus.setGenCode(AclfGenCode.NON_GEN);
		}

		if (xmlBusData.getLoadData() != null) {
			mapLoadData(xmlBusData.getLoadData());
			/* there is no need to do the check. the mapLoadData() method should do the job
			if(xmlBusData.getLoadData().getEquivLoad().getValue().getCode()!=LFLoadCodeEnumType.NONE_LOAD)
			   mapLoadData(xmlBusData.getLoadData());
			else 
				aclfBus.setLoadCode(AclfLoadCode.NON_LOAD);
			*/	
		} else {
			bus.setLoadCode(AclfLoadCode.NON_LOAD);
		}

		if (xmlBusData.getShuntYData() != null && xmlBusData.getShuntYData().getEquivY() != null) {
			YXmlType shuntY = xmlBusData.getShuntYData().getEquivY();
//			byte unit = shuntY.getUnit() == YUnitType.MVAR? UnitType.mVar : UnitType.PU;
			UnitType unit = ToYUnit.f(shuntY.getUnit());
			Complex ypu = UnitHelper.yConversion(new Complex(shuntY.getRe(), shuntY.getIm()),
					bus.getBaseVoltage(), aclfNet.getBaseKva(), unit, UnitType.PU);
			//System.out.println("----------->" + shuntY.getIm() + ", " + shuntY.getUnit() + ", " + ypu.getImaginary());
			bus.setShuntY(ypu);
		}
		
		if(xmlBusData.getShuntCompensator()!=null){
			mapSwitchShuntData(xmlBusData.getShuntCompensator());
		}
	}
	
	private void mapGenData(BusGenDataXmlType xmlGenData) throws InterpssException {
		LoadflowGenDataXmlType xmlDefaultGen = AclfParserHelper.getDefaultGen(xmlGenData);
		VoltageXmlType vXml = xmlDefaultGen.getDesiredVoltage();
		if (xmlDefaultGen.getCode() == LFGenCodeEnumType.PQ) {
			bus.setGenCode(AclfGenCode.GEN_PQ);
			AclfPQGenBus pqBus = bus.toPQBus();
			double p = xmlDefaultGen.getPower().getRe(), 
	               q = xmlDefaultGen.getPower().getIm();
			if (xmlDefaultGen.getPower() != null)
				pqBus.setGen(new Complex(p, q), ToApparentPowerUnit.f(xmlDefaultGen.getPower().getUnit()));
			if (p != 0.0 || q != 0.0) {
				if (xmlDefaultGen.getVoltageLimit() != null) {
			  		final PQBusLimit pqLimit = CoreObjectFactory.createPQBusLimit(bus);
			  		pqLimit.setVLimit(new LimitType(xmlDefaultGen.getVoltageLimit().getMax(), 
			  										xmlDefaultGen.getVoltageLimit().getMin()), 
			  										ToVoltageUnit.f(xmlDefaultGen.getVoltageLimit().getUnit()));						
				}
			}
			else {
				bus.setGenCode(AclfGenCode.NON_GEN);
			}
		} else if (xmlDefaultGen.getCode() == LFGenCodeEnumType.PV &&
				xmlDefaultGen != null) {
			if (xmlDefaultGen.getRemoteVoltageControlBus() == null) {
				bus.setGenCode(AclfGenCode.GEN_PV);
				AclfPVGenBus pvBus = bus.toPVBus();
				//if (xmlEquivGenData == null)
				//	System.out.print(busXmlData);
				if (xmlDefaultGen.getPower() != null) {
					pvBus.setGenP(xmlDefaultGen.getPower().getRe(),
							ToApparentPowerUnit.f(xmlDefaultGen.getPower().getUnit()));
				
					if (vXml == null)
						throw new InterpssException("For Gen PV bus, equivGenData.desiredVoltage has to be defined, busId: " + bus.getId());
					double vpu = UnitHelper.vConversion(vXml.getValue(),
						bus.getBaseVoltage(), ToVoltageUnit.f(vXml.getUnit()), UnitType.PU);
				    //TODO comment out for WECC System QA, to use the input voltage as the PV bus voltage
					pvBus.setDesiredVoltMag(vpu, UnitType.PU);
					
					if (xmlDefaultGen.getQLimit() != null) {
  			  			final PVBusLimit pvLimit = CoreObjectFactory.createPVBusLimit(bus);
  			  			pvLimit.setQLimit(new LimitType(xmlDefaultGen.getQLimit().getMax(), 
  			  										xmlDefaultGen.getQLimit().getMin()), 
  			  									ToReactivePowerUnit.f(xmlDefaultGen.getQLimit().getUnit()));
  			  			pvLimit.setStatus(xmlDefaultGen.getQLimit().isActive());
					}
				}
			}
			else {
				// remote bus voltage
				ipssLogger.fine("Bus is a RemoteQBus, id: " + bus.getId());
				
					bus.setGenCode(AclfGenCode.GEN_PQ);
			  		final AclfPQGenBus gen = bus.toPQBus();
			  		gen.setGen(new Complex(xmlDefaultGen.getPower().getRe(), xmlDefaultGen.getPower().getIm()), 
 					           ToApparentPowerUnit.f(xmlDefaultGen.getPower().getUnit()));
 			   
					String remoteId = BusXmlRef2BusId.fx(xmlDefaultGen.getRemoteVoltageControlBus());
					if (remoteId != null) {
						// TODO : the remote bus might located behind the bus in the ODM file
						// The remote bus to be adjusted is normally defined as a PV bus. It needs to
						// be changed to PQ bus
						// In order to process the info in a late stage, we need to save both aclfBus and xmlEquivGenData objects
						AclfBus remoteBus = aclfNet.getBus(remoteId);
	  					if (remoteBus != null) {
	  						//TODO remoteQ control needs to be changed 
	  	  					if (remoteBus.isGenPV())
	  	  						remoteBus.setGenCode(AclfGenCode.GEN_PQ);

	  	  			  		final RemoteQBus reQBus = CoreObjectFactory.createRemoteQBus(bus, 
	  	  			  				RemoteQControlType.BUS_VOLTAGE, remoteId);
	  	  			  		reQBus.setQLimit(new LimitType(xmlDefaultGen.getQLimit().getMax(), 
	  														xmlDefaultGen.getQLimit().getMin()), 
	  														ToReactivePowerUnit.f(xmlDefaultGen.getQLimit().getUnit()));						
	  	  			  		reQBus.setVSpecified(UnitHelper.vConversion(xmlDefaultGen.getDesiredVoltage().getValue(),
	  								bus.getBaseVoltage(), ToVoltageUnit.f(vXml.getUnit()), UnitType.PU));					
	  					}
					}
			}
		} else if (xmlDefaultGen.getCode() == LFGenCodeEnumType.SWING) {
			bus.setGenCode(AclfGenCode.SWING);
			AclfSwingBus swing = bus.toSwingBus();
			double vpu = UnitHelper.vConversion(vXml.getValue(),
					bus.getBaseVoltage(), ToVoltageUnit.f(vXml.getUnit()), UnitType.PU);
			AngleXmlType angXml = xmlDefaultGen.getDesiredAngle(); 
			double angRad = UnitHelper.angleConversion(angXml.getValue(),
					ToAngleUnit.f(angXml.getUnit()), UnitType.Rad);				
			swing.setDesiredVoltMag(vpu, UnitType.PU);
			swing.setDesiredVoltAng(angRad, UnitType.Rad);		
			if (xmlDefaultGen.getPower() != null) {
				double pPU = UnitHelper.pConversion(xmlDefaultGen.getPower().getRe(), aclfNet.getBaseKva(), 
						ToApparentPowerUnit.f(xmlDefaultGen.getPower().getUnit()), UnitType.PU);
				swing.getBus().setGenP(pPU);
			}
		} else {
			bus.setGenCode(AclfGenCode.NON_GEN);
		}
		
		if (xmlDefaultGen.getMwControlParticipateFactor() != null)
			bus.setGenPartFactor(xmlDefaultGen.getMwControlParticipateFactor());
		
		//map contributing generator data 
		mapContributeGenListData(xmlGenData);
	}
	
	private void mapContributeGenListData(BusGenDataXmlType xmlGenData) throws InterpssException{
		LoadflowGenDataXmlType xmlDefaultGen = AclfParserHelper.getDefaultGen(xmlGenData);
		
		double baseKva = bus.getNetwork().getBaseKva();
		if(xmlGenData.getContributeGen()!=null){
			/*
			 * in general, gen code is defined at the equivGen level.
			 */
			LFGenCodeEnumType genCode = xmlDefaultGen.getCode();
			int genCnt = 1;
			for(JAXBElement<? extends LoadflowGenDataXmlType> elem :xmlGenData.getContributeGen()){
				if(elem!=null){
					
				LoadflowGenDataXmlType xmlGen= elem.getValue();
				
				//Map load flow generator data
				AclfGen gen= this.bus instanceof DStabBus? DStabObjectFactory.createDStabGen() :
								this.bus instanceof AcscBus? CoreObjectFactory.createAcscGen() : 
									CoreObjectFactory.createAclfGen();
				gen.setId(xmlGen.getId()!=null?xmlGen.getId():this.bus.getId()+"-G"+genCnt++);
				
				gen.setStatus(xmlGen.isOffLine()==null?true:!xmlGen.isOffLine());
				/*
				double Mva =xmlGen.getMvaBase().getValue();
				double MvaFactor = xmlGen.getMvaBase().getUnit()==ApparentPowerUnitType.MVA?1.0:
					    xmlGen.getMvaBase().getUnit()==ApparentPowerUnitType.KVA?1.0E-3:
						xmlGen.getMvaBase().getUnit()==ApparentPowerUnitType.VA?1.0E-6:
							100.0; //PU, assuming 100 MVA Base
				*/
				gen.setCode(genCode == LFGenCodeEnumType.SWING? AclfGenCode.SWING : 
								genCode == LFGenCodeEnumType.PQ? AclfGenCode.GEN_PQ :
									genCode == LFGenCodeEnumType.PV? AclfGenCode.GEN_PV : AclfGenCode.NON_GEN);
				
				double genMva = xmlGen.getMvaBase() != null? xmlGen.getMvaBase().getValue() : baseKva*0.001;
				gen.setMvaBase(genMva);
				
				if (xmlGen.getDesiredVoltage() != null)
					gen.setDesiredVoltMag(UnitHelper.vConversion(xmlGen.getDesiredVoltage().getValue(),
						bus.getBaseVoltage(), ToVoltageUnit.f(xmlGen.getDesiredVoltage().getUnit()), UnitType.PU));
				
				PowerXmlType genPower = xmlGen.getPower();
				
				Complex genPQ= genPower!=null? new Complex(genPower.getRe(),genPower.getIm()) : new Complex(0.0,0.0);
				/*
				double genFactor = genPower.getUnit()==ApparentPowerUnitType.MVA?1.0E-2:
							genPower.getUnit()==ApparentPowerUnitType.KVA?1.0E-5:
								genPower.getUnit()==ApparentPowerUnitType.VA?1.0E-8:
								1.0; //PU, assuming 100 MVA Base
				*/
				
				//AclfGen power is defined in pu, system MVA-based
				gen.setGen(UnitHelper.pConversion(genPQ, baseKva, 
						ToApparentPowerUnit.f(xmlGen.getPower()==null?ApparentPowerUnitType.PU:xmlGen.getPower().getUnit()), UnitType.PU ));
				
				if(xmlGen.getSourceZ()!=null)
				gen.setSourceZ(new Complex(xmlGen.getSourceZ().getRe(),xmlGen.getSourceZ().getIm()));
				
				// generator step-up transformer: z = 0, Tap =1.0 by default, which means
				// the transformer is modeled separately.
				if(xmlGen.getXfrZ()!=null){
				  if(xmlGen.getXfrZ().getIm()!=0 ||xmlGen.getXfrZ().getRe()!=0){
				      gen.setXfrZ(new Complex(xmlGen.getXfrZ().getRe(),xmlGen.getXfrZ().getIm()));
				      gen.setXfrTap(xmlGen.getXfrTap() != null? xmlGen.getXfrTap() : 1.0);
			        }
				}
				//AclfGen active power limit is defined in pu, system MVA-based
				if(xmlGen.getPLimit()!=null)
				gen.setPGenLimit(UnitHelper.pConversion( new LimitType(xmlGen.getPLimit().getMax(),xmlGen.getPLimit().getMin()), 
						baseKva, ToActivePowerUnit.f(xmlGen.getPLimit().getUnit()), UnitType.PU ));
				
				//AclfGen reactive power limit
				if(xmlGen.getQLimit()!=null)
				gen.setQGenLimit(UnitHelper.pConversion( new LimitType(xmlGen.getQLimit().getMax(),xmlGen.getQLimit().getMin()), 
						baseKva, ToReactivePowerUnit.f(xmlGen.getQLimit().getUnit()), UnitType.PU ));
				
				if(xmlGen.getRemoteVoltageControlBus()!=null){
				String remoteId = BusXmlRef2BusId.fx(xmlGen.getRemoteVoltageControlBus());
				gen.setRemoteVControlBusId(remoteId);
				}
				
				
				gen.setMvarControlPFactor(xmlGen.getMvarVControlParticipateFactor()!=null?xmlGen.getMvarVControlParticipateFactor():1.0);
				
				//MW pf is optional
				gen.setMwControlPFactor(xmlGen.getMwControlParticipateFactor()!=null?xmlGen.getMwControlParticipateFactor():1.0);
				
				//add the generator to the bus GenList
				bus.getGenList().add(gen);
				
				}
			}
		}
	}
	
	
	private void mapLoadData(BusLoadDataXmlType xmlLoadData) throws InterpssException {
		double baseKva = bus.getNetwork().getBaseKva();
		bus.setLoadCode(AclfLoadCode.NON_LOAD);
		
		/*
		 * we assume that the equivLoad override the contributing load list
		 */
		LoadflowLoadDataXmlType xmlDefaultLoad = AclfParserHelper.getDefaultLoad(xmlLoadData);
		if (xmlDefaultLoad != null && xmlDefaultLoad.getCode() != null) {
			// to detect the <equivLoad/> situation
			//Bus load code
			LFLoadCodeEnumType code = xmlDefaultLoad.getCode();
			bus.setLoadCode(code == LFLoadCodeEnumType.CONST_I ? AclfLoadCode.CONST_I : 
				(code == LFLoadCodeEnumType.CONST_Z ? AclfLoadCode.CONST_Z : 
					(code == LFLoadCodeEnumType.CONST_P  ? AclfLoadCode.CONST_P : 
						 code == LFLoadCodeEnumType.FUNCTION_LOAD?AclfLoadCode.ZIP:
						AclfLoadCode.NON_LOAD)));

			AclfLoadBusAdapter loadBus = bus.toLoadBus();

			//LoadflowLoadDataXmlType xmlEquivLoad = xmlLoadData.getEquivLoad().getValue();
			if (xmlDefaultLoad != null) {
				if (code == LFLoadCodeEnumType.FUNCTION_LOAD) {
					// 1) When code = FunctionLoad, the ZIP load units should be the same
					// 2) the p, i, z element is optional
					PowerXmlType p = xmlDefaultLoad.getConstPLoad(),
								 i = xmlDefaultLoad.getConstILoad(),
								 z = xmlDefaultLoad.getConstZLoad();
					double re = 0.0, im = 0.0;
					ApparentPowerUnitType unit = null;
					if (p != null) {
						unit = p.getUnit();
						re += p.getRe();
						im += p.getIm();
					}
					
					if (i != null) {
						if (unit == null) 
							unit = i.getUnit();
						if (unit != i.getUnit()) 
							throw new InterpssException("Inconsitent FunctionLoad power unit");
						re += i.getRe();
						im += i.getIm();
					}

					if (z != null) {
						if (unit == null) 
							unit = z.getUnit();
						if (unit != z.getUnit()) 
							throw new InterpssException("Inconsitent FunctionLoad power unit");					
						re += z.getRe();
						im += z.getIm();
					}
					
					 // P = P0 * [ Ap + Bp * V + Cp * V*V ], where Cp = 1.0 - Ap - Bp
	                 // Q = Q0 * [ Aq + Bq * V + Cq * V*V ], where Cq = 1.0 - Aq - Bq
					 //
					loadBus.setLoad(new Complex(re, im), ToApparentPowerUnit.f(unit));
			  		FunctionLoad fl = CoreObjectFactory.createFunctionLoad(bus);
			  		if (re != 0.0) {
			  			if (p != null)
			  				fl.getP().setA(p.getRe()/re);
			  			if (i != null)
			  				fl.getP().setB(i.getRe()/re);
			  		}
			  		if (im != 0.0) {
			  			if (p != null)
			  				fl.getQ().setA(p.getIm()/im);
			  			if (i != null)
			  				fl.getQ().setB(i.getIm()/im);
			  		}
				}
				else {
					PowerXmlType p = null;
					if (bus.getLoadCode() == AclfLoadCode.CONST_P)
						p = xmlDefaultLoad.getConstPLoad();
					else if (bus.getLoadCode() == AclfLoadCode.CONST_I)
						p = xmlDefaultLoad.getConstILoad();
					else if (bus.getLoadCode() == AclfLoadCode.CONST_Z)	
						p = xmlDefaultLoad.getConstZLoad();
					
					if (p != null)
						loadBus.setLoad(new Complex(p.getRe(), p.getIm()), ToApparentPowerUnit.f(p.getUnit()));				
				}
			}
		}
		else {
	         // map each connecting load
			if(xmlLoadData.getContributeLoad()!=null){
				if(xmlLoadData.getContributeLoad().size()>0){
					// we set parent bus load code to constant power
					bus.setLoadCode(AclfLoadCode.CONST_P);
					for(JAXBElement<? extends LoadflowLoadDataXmlType> elem: xmlLoadData.getContributeLoad()){
						if(elem!=null){
							LoadflowLoadDataXmlType loadElem = elem.getValue();
							
							AclfLoad load = CoreObjectFactory.createAclfLoad();
							bus.getLoadList().add(load);
							//status
							load.setStatus(loadElem.isOffLine()!=null?!loadElem.isOffLine():true);
						    // load code		
							AclfLoadCode code = AclfLoadCode.NON_LOAD;
							PowerXmlType p = loadElem.getConstPLoad(),
										 i = loadElem.getConstILoad(),
										 z = loadElem.getConstZLoad();

							if (p != null) {
								code = code == AclfLoadCode.NON_LOAD? AclfLoadCode.CONST_P : AclfLoadCode.ZIP;
								load.setLoadCP(UnitHelper.pConversion( new Complex(p.getRe(),p.getIm()), 
										baseKva, ToApparentPowerUnit.f(p.getUnit()), UnitType.PU ));
							}
							
							if (i != null) {
								code = code == AclfLoadCode.NON_LOAD? AclfLoadCode.CONST_I : AclfLoadCode.ZIP;
								load.setLoadCI(UnitHelper.pConversion( new Complex(i.getRe(),i.getIm()), 
										baseKva, ToApparentPowerUnit.f(i.getUnit()), UnitType.PU ));
							}

							if (z != null) {
								code = code == AclfLoadCode.NON_LOAD? AclfLoadCode.CONST_Z : AclfLoadCode.ZIP;
								load.setLoadCZ(UnitHelper.pConversion( new Complex(z.getRe(),z.getIm()), 
										baseKva, ToApparentPowerUnit.f(z.getUnit()), UnitType.PU ));
							}

							load.setCode(code);
					   }
				   }
			   }
			}
		}
	}
	
	private void mapSwitchShuntData(ShuntCompensatorXmlType xmlSwitchedShuntData){
		//TODO 
		SwitchedShunt swchShunt = CoreObjectFactory.createSwitchedShunt();
		//swithced shunt is a also a AclfControlBus
		this.bus.setBusControl(swchShunt);
		
		ReactivePowerXmlType binit = xmlSwitchedShuntData.getBInit();
		
		if (binit != null) {
			//cacluate the factor to convert binit to pu based.
			double factor = binit.getUnit()==ReactivePowerUnitType.PU?1.0:
				             binit.getUnit()==ReactivePowerUnitType.MVAR?0.01:
				            	 binit.getUnit()==ReactivePowerUnitType.KVAR?1.0E-5:
				            		 1.0E-8; // VAR->1.0E-8 with a 100 MVA base
			
			swchShunt.setBInit(binit.getValue()*factor);

			VarCompensatorControlMode mode = xmlSwitchedShuntData.getMode()==ShuntCompensatorModeEnumType.CONTINUOUS?
					VarCompensatorControlMode.CONTINUOUS:xmlSwitchedShuntData.getMode()==ShuntCompensatorModeEnumType.DISCRETE?
					VarCompensatorControlMode.DISCRETE:VarCompensatorControlMode.FIXED;
			
			swchShunt.setControlMode(mode);
			
			LimitType vLimit = new LimitType(xmlSwitchedShuntData.getDesiredVoltageRange().getMax(),
					xmlSwitchedShuntData.getDesiredVoltageRange().getMin());
			//TODO vLimit is missing
			//swchShunt.set
			for(ShuntCompensatorBlockXmlType varBankXml:xmlSwitchedShuntData.getBlock()){
				QBank varBank= CoreObjectFactory.createQBank();
				swchShunt.getVarBankArray().add(varBank);
				
				varBank.setSteps(varBankXml.getSteps());
				ReactivePowerXmlType unitVarXml = varBankXml.getIncrementB();
				
				factor = unitVarXml.getUnit()==ReactivePowerUnitType.PU?1.0:
					unitVarXml.getUnit()==ReactivePowerUnitType.MVAR?1.0E-2:
						unitVarXml.getUnit()==ReactivePowerUnitType.KVAR?1.0E-5:
		            		 1.0E-8; 
				//TODO UnitQMVar is in pu
				varBank.setUnitQMvar(unitVarXml.getValue()*factor);
				
			}
		}
	}
}
