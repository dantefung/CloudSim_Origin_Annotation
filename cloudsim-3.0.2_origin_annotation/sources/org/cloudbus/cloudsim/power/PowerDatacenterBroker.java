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
	 *设计思想：
	 *	通过构造函数初始化一些参数和需要的资源。
	 *	
	 *	当PowerDatacenter类的对象创立之时，同时通过构造函数初始化一些参数。
	 *	这里PowerDatacenter实体在初始化this.name = name;时，同时将自己这个实体的对象记录在CloudSim类中的entity列表中。
	 *	待到CloudSim类中的runOnTick();
	 *
	 *	调用的层级关系：
	 *  PowerDatacenterBroker.java-->DatacenterBroker.java-->SimEntiy.java中的构造函数。
	 * @param name the name
	 * @throws Exception the exception
	 */
	public PowerDatacenterBroker(String name) throws Exception {
		super(name);
		/**
		 * 调用父类DatacenterBroker的构造函数。
		 * SimEntity.java:public SimEntity(String name){...CloudSim.addEntity(this);}
		 * -->CloudSim.java:public static void addEntity(SimEntity e){...entities.add(e);entitiesByName.put(e.getName(), e);}
		 */
	}

	/*
	 * 调用层级关系：
	 * 	CloudSim.java(
	 * runClockTick()
	 * -->从entity列表取出的ent，即PowerDatacerBroker对象:ent.run();
	 * -->本类，PowerDatacenterBroker对象的processEvent(ev)方法。
	 * -->/*VM Creation answer /case CloudSimTags.VM_CREATE_ACK:processVmCreate(ev);break;)
	 * 
	 *  这里是PowerDatacenterBroker类对象调用本类中所覆写父类的processVmCreate（）方法。即如下方法。
	 * (non-Javadoc)
	 * @see
	 * org.cloudbus.cloudsim.DatacenterBroker#processVmCreate(org.cloudbus.cloudsim.core.SimEvent)
	 */
	@Override
	protected void processVmCreate(SimEvent ev) {
		int[] data = (int[]) ev.getData();//Object類型強制轉換為int[]類型的數據。
		int result = data[2];//1或0，成功或失败。

		/**
		 * 分析上下代码可知：
		 * 数组内存储的数据分别为：数据中心的ID、虚拟机的ID、虚拟机的是否创建成功。
		 * 
		 */
		if (result != CloudSimTags.TRUE) {//如果虚拟机在数据中心创建不成功。
			int datacenterId = data[0];
			int vmId = data[1];
			System.out.println(CloudSim.clock() + ": " + getName() + ": Creation of VM #" + vmId
					+ " failed in Datacenter #" + datacenterId);
			System.exit(0);//这个方法是用来结束当前正在运行中的java虚拟机；0-正常結束程序；1-異常結束程序。
			//在一个if-else判断中，如果我们程序是按照我们预想的执行，到最后我们需要停止程序，那么我们使用System.exit(0)
		}
		/**
		 * 调用父类DatacenterBroker的processVmCreate()方法。
		 * 
		 * 
		 */
		super.processVmCreate(ev);//若以上的if(){...}执行则这里调用父类的方法不执行。因为System.exit(0)是将你的整个虚拟机里的内容都停掉了，无论如何，内存都释放了！也就是说连JVM都关闭了，内存里根本不可能还有什么东西。
	}

}
