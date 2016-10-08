/*
 * Title:        CloudSim Toolkit
 * Description:  CloudSim (Cloud Simulation) Toolkit for Modeling and Simulation of Clouds
 * Licence:      GPL - http://www.gnu.org/copyleft/gpl.html
 *
 * Copyright (c) 2009-2012, The University of Melbourne, Australia
 */

package org.cloudbus.cloudsim.core.predicates;

import org.cloudbus.cloudsim.core.SimEvent;

/**
 * A predicate to select events with specific tags.
 * һ�����ʣ����ԡ�ν�ʣ�����ѡ����������ǩ���¼���
 * 
 * @author Marcos Dias de Assuncao
 * @since CloudSim Toolkit 1.0
 * @see PredicateNotType
 * @see Predicate
 */
public class PredicateType extends Predicate {

	/** The tags. */
	private final int[] tags;

	/**
	 * Constructor used to select events with the tag value <code>t1</code>.
	 * ���췽��������ѡ����б�ǩֵ���¼���
	 * 
	 * ���˼�룺���ڽ��յ�������ı�ǩ��
	 * 
	 * @param t1 an event tag value
	 */
	public PredicateType(int t1) {
		tags = new int[] { t1 };
	}

	/**
	 * Constructor used to select events with a tag value equal to any of the specified tags.
	 * ���췽��������ѡ����е����κι涨�õı�ǩ��ֵ���¼���
	 * 
	 * ���˼�룺���ڽ��ն������ı�ǩ��
	 * 
	 * Constructor ���캯����ͨ��������ʼ�����ݳ�Ա���������Դ��
	 * 
	 * @param tags the list of tags
	 */
	public PredicateType(int[] tags) {
		this.tags = tags.clone();
	}

	/**
	 * The match function called by <code>Sim_system</code>, not used directly by the user.
	 * 
	 * @param ev the ev
	 * @return true, if match
	 */
	@Override
	public boolean match(SimEvent ev) {
		int tag = ev.getTag();
		for (int tag2 : tags) {
			if (tag == tag2) {
				return true;
			}
		}
		return false;
	}

}
