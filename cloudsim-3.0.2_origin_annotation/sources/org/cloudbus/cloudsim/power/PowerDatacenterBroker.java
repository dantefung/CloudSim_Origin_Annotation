/*
 * Title:        CloudSim Toolkit
 * Description:  CloudSim (Cloud Simulation) Toolkit for Modeling and Simulation of Clouds
 * Licence:      GPL - http://www.gnu.org/copyleft/gpl.html
 *
 * Copyright (c) 2009-2012, The University of Melbourne, Australia
 */

package org.cloudbus.cloudsim.power;

import org.cloudbus.cloudsim.DatacenterBroker;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.core.CloudSimTags;
import org.cloudbus.cloudsim.core.SimEntity;
import org.cloudbus.cloudsim.core.SimEvent;

/**
 * A broker for the power package.
 * 
 * If you are using any algorithms, policies or workload included in the power package please cite
 * the following paper:
 * 
 * Anton Beloglazov, and Rajkumar Buyya, "Optimal Online Deterministic Algorithms and Adaptive
 * Heuristics for Energy and Performance Efficient Dynamic Consolidation of Virtual Machines in
 * Cloud Data Centers", Concurrency and Computation: Practice and Experience (CCPE), Volume 24,
 * Issue 13, Pages: 1397-1420, John Wiley & Sons, Ltd, New York, USA, 2012
 * 
 * @author Anton Beloglazov
 * @since CloudSim Toolkit 2.0
 */
public class PowerDatacenterBroker extends DatacenterBroker {

	/**
	 * Instantiates a new power datacenter broker.
	 *
	 *
	 *���˼�룺
	 *	ͨ�����캯����ʼ��һЩ��������Ҫ����Դ��
	 *	
	 *	��PowerDatacenter��Ķ�����֮ʱ��ͬʱͨ�����캯����ʼ��һЩ������
	 *	����PowerDatacenterʵ���ڳ�ʼ��this.name = name;ʱ��ͬʱ���Լ����ʵ��Ķ����¼��CloudSim���е�entity�б��С�
	 *	����CloudSim���е�runOnTick();
	 *
	 *	���õĲ㼶��ϵ��
	 *  PowerDatacenterBroker.java-->DatacenterBroker.java-->SimEntiy.java�еĹ��캯����
	 * @param name the name
	 * @throws Exception the exception
	 */
	public PowerDatacenterBroker(String name) throws Exception {
		super(name);
		/**
		 * ���ø���DatacenterBroker�Ĺ��캯����
		 * SimEntity.java:public SimEntity(String name){...CloudSim.addEntity(this);}
		 * -->CloudSim.java:public static void addEntity(SimEntity e){...entities.add(e);entitiesByName.put(e.getName(), e);}
		 */
	}

	/*
	 * ���ò㼶��ϵ��
	 * 	CloudSim.java(
	 * runClockTick()
	 * -->��entity�б�ȡ����ent����PowerDatacerBroker����:ent.run();
	 * -->���࣬PowerDatacenterBroker�����processEvent(ev)������
	 * -->/*VM Creation answer /case CloudSimTags.VM_CREATE_ACK:processVmCreate(ev);break;)
	 * 
	 *  ������PowerDatacenterBroker�������ñ���������д�����processVmCreate���������������·�����
	 * (non-Javadoc)
	 * @see
	 * org.cloudbus.cloudsim.DatacenterBroker#processVmCreate(org.cloudbus.cloudsim.core.SimEvent)
	 */
	@Override
	protected void processVmCreate(SimEvent ev) {
		int[] data = (int[]) ev.getData();//Object��͏����D�Q��int[]��͵Ĕ�����
		int result = data[2];//1��0���ɹ���ʧ�ܡ�

		/**
		 * �������´����֪��
		 * �����ڴ洢�����ݷֱ�Ϊ���������ĵ�ID���������ID����������Ƿ񴴽��ɹ���
		 * 
		 */
		if (result != CloudSimTags.TRUE) {//�����������������Ĵ������ɹ���
			int datacenterId = data[0];
			int vmId = data[1];
			System.out.println(CloudSim.clock() + ": " + getName() + ": Creation of VM #" + vmId
					+ " failed in Datacenter #" + datacenterId);
			System.exit(0);//�������������������ǰ���������е�java�������0-�����Y������1-�����Y������
			//��һ��if-else�ж��У�������ǳ����ǰ�������Ԥ���ִ�У������������Ҫֹͣ������ô����ʹ��System.exit(0)
		}
		/**
		 * ���ø���DatacenterBroker��processVmCreate()������
		 * 
		 * 
		 */
		super.processVmCreate(ev);//�����ϵ�if(){...}ִ����������ø���ķ�����ִ�С���ΪSystem.exit(0)�ǽ�������������������ݶ�ͣ���ˣ�������Σ��ڴ涼�ͷ��ˣ�Ҳ����˵��JVM���ر��ˣ��ڴ�����������ܻ���ʲô������
	}

}
