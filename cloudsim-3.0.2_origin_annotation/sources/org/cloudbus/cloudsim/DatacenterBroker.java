/*
 * Title:        CloudSim Toolkit
 * Description:  CloudSim (Cloud Simulation) Toolkit for Modeling and Simulation of Clouds
 * Licence:      GPL - http://www.gnu.org/copyleft/gpl.html
 *
 * Copyright (c) 2009-2012, The University of Melbourne, Australia
 */

package org.cloudbus.cloudsim;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.core.CloudSimTags;
import org.cloudbus.cloudsim.core.SimEntity;
import org.cloudbus.cloudsim.core.SimEvent;
import org.cloudbus.cloudsim.lists.CloudletList;
import org.cloudbus.cloudsim.lists.VmList;

/**
 * DatacentreBroker represents a broker acting on behalf of a user. It hides VM management, as vm
 * creation, sumbission of cloudlets to this VMs and destruction of VMs.
 * 
 * @author Rodrigo N. Calheiros
 * @author Anton Beloglazov
 * @since CloudSim Toolkit 1.0
 */
public class DatacenterBroker extends SimEntity {

	/** The vm list. */
	protected List<? extends Vm> vmList;

	/** The vms created list. */
	protected List<? extends Vm> vmsCreatedList;

	/** The cloudlet list. */
	protected List<? extends Cloudlet> cloudletList;

	/** The cloudlet submitted list. */
	protected List<? extends Cloudlet> cloudletSubmittedList;

	/** The cloudlet received list. */
	protected List<? extends Cloudlet> cloudletReceivedList;

	/** The cloudlets submitted. */
	protected int cloudletsSubmitted;

	/** The vms requested. */
	protected int vmsRequested;

	/** The vms acks. */
	protected int vmsAcks;

	/** The vms destroyed. */
	protected int vmsDestroyed;

	/** The datacenter ids list. */
	protected List<Integer> datacenterIdsList;

	/** The datacenter requested ids list. */
	protected List<Integer> datacenterRequestedIdsList;

	/** The vms to datacenters map. */
	protected Map<Integer, Integer> vmsToDatacentersMap;

	/** The datacenter characteristics list. */
	protected Map<Integer, DatacenterCharacteristics> datacenterCharacteristicsList;

	/**
	 * Created a new DatacenterBroker object.
	 * 
	 * @param name name to be associated with this entity (as required by Sim_entity class from
	 *            simjava package)
	 * @throws Exception the exception
	 * @pre name != null
	 * @post $none
	 */
	public DatacenterBroker(String name) throws Exception {
		super(name);

		setVmList(new ArrayList<Vm>());
		setVmsCreatedList(new ArrayList<Vm>());
		setCloudletList(new ArrayList<Cloudlet>());
		setCloudletSubmittedList(new ArrayList<Cloudlet>());
		setCloudletReceivedList(new ArrayList<Cloudlet>());

		cloudletsSubmitted = 0;
		setVmsRequested(0);
		setVmsAcks(0);
		setVmsDestroyed(0);

		setDatacenterIdsList(new LinkedList<Integer>());
		setDatacenterRequestedIdsList(new ArrayList<Integer>());
		setVmsToDatacentersMap(new HashMap<Integer, Integer>());
		setDatacenterCharacteristicsList(new HashMap<Integer, DatacenterCharacteristics>());
	}

	/**
	 * This method is used to send to the broker the list with virtual machines that must be
	 * created.
	 * 
	 * @param list the list
	 * @pre list !=null
	 * @post $none
	 */
	public void submitVmList(List<? extends Vm> list) {
		getVmList().addAll(list);
	}

	/**
	 * This method is used to send to the broker the list of cloudlets.
	 * 
	 * @param list the list
	 * @pre list !=null
	 * @post $none
	 */
	public void submitCloudletList(List<? extends Cloudlet> list) {
		getCloudletList().addAll(list);
	}

	/**
	 * Specifies that a given cloudlet must run in a specific virtual machine.
	 * 指定一个给定的云任务必须运行在一个指定的虚拟机上。
	 * 
	 * @param cloudletId ID of the cloudlet being bount to a vm
	 * @param vmId the vm id
	 * @pre cloudletId > 0
	 * @pre id > 0
	 * @post $none
	 */
	public void bindCloudletToVm(int cloudletId, int vmId) {
		CloudletList.getById(getCloudletList(), cloudletId).setVmId(vmId);//从云任务的等待列表里取出指定云任务，然后将该任务与特定的虚拟机进行绑定。
	}

	/**
	 * Processes events available for this Broker.
	 * 
	 * @param ev a SimEvent object
	 * @pre ev != null
	 * @post $none
	 */
	@Override
	public void processEvent(SimEvent ev) {
		switch (ev.getTag()) {
		// Resource characteristics request
			case CloudSimTags.RESOURCE_CHARACTERISTICS_REQUEST:
				processResourceCharacteristicsRequest(ev);//处理自己发送给自己的一个消息。
				break;
			// Resource characteristics answer
			case CloudSimTags.RESOURCE_CHARACTERISTICS:
				processResourceCharacteristics(ev);//處理數據中心對DatacenterBroker請求數據中心的一些基礎設施的請求，發送回來的迴應事件。
				break;
			// VM Creation answer
			case CloudSimTags.VM_CREATE_ACK:
				processVmCreate(ev);
				break;
			// A finished cloudlet returned
			case CloudSimTags.CLOUDLET_RETURN:
				processCloudletReturn(ev);
				break;
			// if the simulation finishes
			case CloudSimTags.END_OF_SIMULATION:
				shutdownEntity();
				break;
			// other unknown tags are processed by this method
			default:
				processOtherEvent(ev);
				break;
		}
	}

	/**
	 * Process the return of a request for the characteristics of a PowerDatacenter.
	 * DatacenterBroker处理(数据中心针对其基础硬件资源的请求所给予的)回应。
	 * 
	 * 调用层级关系：
	 * 创建一个DatacenterBroker（DatacenterBroker）对象时，通过自己构造函数，调用父类的构造函数设置了name的同时，将自己的对象存放在CloudSim中的entities列表中。
	 * CloudSim.java(
	 * startSimulation();
	 * -->run()
	 * -->runStart()：从entity列表中迭代出实体，逐个启动。
	 * -->startEntity()：对DatacenterBroker而言，schedule(getId(), 0, CloudSimTags.RESOURCE_CHARACTERISTICS_REQUEST);这个消息会被封装成一个事件，放入future队列，再将从future队列调入deffered队列，到时是自己接收处理，protected void processResourceCharacteristicsRequest(SimEvent ev) {...}。
	 * -->runClockTick()
	 * -->从entity列表取出的ent，即PowerDatacerBroker对象:ent.run();
	 * -->从延时队列中取出该实体能处理的事件。
	 * -->本类，PowerDatacenterBroker对象（DatacenterBroker对象）的processEvent(ev)方法。
	 * -->case CloudSimTags.RESOURCE_CHARACTERISTICS:processResourceCharacteristics(ev);
	 * 
	 * 
	 * @param ev a SimEvent object
	 * @pre ev != $null
	 * @post $none
	 */
	 protected void processResourceCharacteristics(SimEvent ev) {
		DatacenterCharacteristics characteristics = (DatacenterCharacteristics) ev.getData();//从数据中心发送回来的回应事件内取出该数据中心的基础硬件资源对象。
		getDatacenterCharacteristicsList().put(characteristics.getId(), characteristics);//将数据中心的基础硬件资源(数据中心特征)对象存入map集合。

		if (getDatacenterCharacteristicsList().size() == getDatacenterIdsList().size()) {//在CloudSim.java(runClockTick()：迭代出每个实体-->ent.run()：while{..}不断地从deffered queue中取出事件进行处理。-->processResourceCharacteristics（）)
			setDatacenterRequestedIdsList(new ArrayList<Integer>());//创建一个列表用来在下一步尝试在数据中心创建虚拟机后，记录已经被请求过的数据中心。/*实现创建好一个请求过的数据中心列表。createVmsInDatacenter(getDatacenterIdsList().get(0))将会用到该列表*/
			createVmsInDatacenter(getDatacenterIdsList().get(0));//在该数据中心里创建虚拟机。
		}
	}

	/**
	 * Process a request for the characteristics of a PowerDatacenter.
	 * 
	 *	自己处理自己在启动时发送的一个消息。
	 * 
	 * @param ev a SimEvent object
	 * @pre ev != $null
	 * @post $none
	 */
	protected void processResourceCharacteristicsRequest(SimEvent ev) {
		setDatacenterIdsList(CloudSim.getCloudResourceList());//设置云资源列表。
		                                                      /*	调用层级关系：
		                                                       * init()：CloudSim初始化，...创建CIS对象
		                                                       * -->startSimulation()-->run()
		                                                       * -->runStart()-->startEntity()：实体CloudSimShutDown、CloudInformationService、Datacenter、DatacenterBroker启动，各自开始发送事件。
		                                                       * 其中 Datacenter:sendNow(gisID, CloudSimTags.REGISTER_RESOURCE, getId());
		                                                       * -->runClockTick()
		                                                       * -->从entity列表取出的ent，即CloudInformationService对象:ent.run();
		                                                       * -->从延时队列中取出该实体能处理的事件。
		                                                       * -->本类，CloudInformationService对象的processEvent(ev)方法。
		                                                       * -->case CloudSimTags.REGISTER_RESOURCE:resList.add((Integer) ev.getData()); 此时，Datacenter对象存入了云信息服务中心的资源列表里。
		                                                       * -->...
		                                                       * -->CloudSim.getCloudResourceList();
		                                                       * -->return cis.getList();
		                                                       * -->将cis.getList赋值给datacenterIdsList.
		                                                       * */
		setDatacenterCharacteristicsList(new HashMap<Integer, DatacenterCharacteristics>());//事先创建好一个map集合，protected void processResourceCharacteristics(SimEvent ev) {..}，即上面的代码块将会用到。将数据中心的基础硬件资源(数据中心特征)对象存入map集合。

		Log.printLine(CloudSim.clock() + ": " + getName() + ": Cloud Resource List received with "
				+ getDatacenterIdsList().size() + " resource(s)");//打印出：xx时刻：DatacenterBroker1：云资源列表接收到xx资源。

		for (Integer datacenterId : getDatacenterIdsList()) {//从可用的数据中心列表中迭代出所有的数据中心的ID
			sendNow(datacenterId, CloudSimTags.RESOURCE_CHARACTERISTICS, getId());//向所有可用的数据中心发送一个消息，请求获取该数据中心的基础硬件资源。
		}//此时，future队列中，有getDatacenterIdsList().size()个由DatacenterBroker发出的事件.CloudSim中的run(){.. while(true){.. runClockTick(){..for(){ent.run( while(){processingEvent()；//单个实体从deffered队列中所有应由他处理的事件，直到事件处理完了之后，才轮到下一个实体从deffered队列中寻找自己要处理的所有事件，一个一个处理完。})} .. }..}.. }本次while大循环做了两件事：1.处理了之前deffered队列里面的事件。2.将future队列里面的事件移入deffered队列里面。等下一次大while（）循环时，Datacenter会一个一个地将deffered队列中的与自己相关联的事件都处理完。
	}

	/**
	 * Process the ack received due to a request for VM creation.
	 * 处理已经接收到的且是由于一个虚拟机创建请求所导致（产生）的ack（响应、回应、应答）.
	 * 
	 * 实际含义：
	 * 	DatacenterBroker发送一个创建Vm的请求给Datacenter，Datacenter接收到这个请求，开始处理并发送回一个应答ack告诉DatacenterBroker虚拟机的是否创建成功。
	 * 而这里，processVmCreate（）是由DatacenterBroker收到了Datacenter反馈ack回来的信息，然后开始进行处理（做出相应的动作或反应）。
	 * 
	 * be due to <=> be caused by
	 * 
	 *  这里due to 作为the ack的后置定语。
	 * 
	 * @param ev a SimEvent object
	 * @pre ev != null
	 * @post $none
	 */
	protected void processVmCreate(SimEvent ev) {
		int[] data = (int[]) ev.getData();//Object data,存储任意的数据，这里是存储了一个数组。
		int datacenterId = data[0];//数据中心的ID
		int vmId = data[1];//在數據中心創建的虚拟机的ID
		int result = data[2];//創建虛擬機是否成功的一個結果。

		/**
		 * New annotation by DanteFung.
		 * 
		 * 這一步是檢驗請求創建的具體某台虛擬機是否都在數據中心創建成功了。
		 * 是。1.用vmsCreatedList列表記錄所有創建好的虛擬機  2.map集合，記錄虛擬機ID和數據中心ID的一個映射。
		 * 否。向控制台輸出具體哪臺虛擬機在具體的哪個數據中心創建失敗。
		 * 
		 */
		if (result == CloudSimTags.TRUE) {//如果虛擬機創建成功
			getVmsToDatacentersMap().put(vmId, datacenterId);//虛擬機在數據中心內創建成功，用一個map集合記錄，虛擬機ID和數據中心的一個映射關係，將虛擬機與其對應的數據中心關聯起來。
			getVmsCreatedList().add(VmList.getById(getVmList(), vmId));//將創建成功的虛擬機記錄在VmsCreatedList列表里。該列表是直接接受一個Vm對象。
			Log.printLine(CloudSim.clock() + ": " + getName() + ": VM #" + vmId
					+ " has been created in Datacenter #" + datacenterId + ", Host #"
					+ VmList.getById(getVmsCreatedList(), vmId).getHost().getId());//根據vmID從vmsCreatedList列表中取出對應的Vm對象==>即得到創建成功的Vm對象==>取出vm對應的那台主機Host的ID. 按UML來理解，vm和host屬於關聯關係。
		} else {//如果創建不成功，控制臺輸出，具體哪台虛擬機在哪個數據中心內創建失敗。
			Log.printLine(CloudSim.clock() + ": " + getName() + ": Creation of VM #" + vmId
					+ " failed in Datacenter #" + datacenterId);
		}

		incrementVmsAcks();//統計得到的迴應數。這個一個迭代的過程。
		
		/**
		 *	上面的代碼塊，是針對單台虛擬機而言的迭代記錄過程。對vmList內的所有虛擬機而言，無論其在數據中心創建成功與否，都會從數據中心處發送回一個反饋信息回來給DatacenterBroker。 
		 * 
		 * 	以下的代碼塊，是在    “存放所有創建成功的虛擬機的列表長度  == 存放被請求創建的所有虛擬機的列表 - 銷燬掉的虛擬機” 成立的情況下才執行。
		 * 	vmsDestroyed是在DatacenterBroker創建對象時，通過構造函數初始化為0；
		 * 
		 * 	意味著，DatacenterBroker所請求的要創建的所有虛擬機，Datacenter全部都創建成功了。
		 * 
		 * 	開始執行下面的代碼塊。
		 * 	
		 */

		// all the requested VMs have been created 所有被要求的虛擬機已經創建成功。
		if (getVmsCreatedList().size() == getVmList().size() - getVmsDestroyed()) {
			submitCloudlets();//提交云任务到已经创建好的虛擬機上。
		} else {//如果不是所有的请求创建的虚拟机都创建好，仍有部分虚拟机创建失败的话，就尝试着在下一个数据中心上创建虚拟机。
			// all the acks received, but some VMs were not created  Datacenter上创建的每一个虚拟机，不论创建成功与否，都会返回一个应答。因此，所有的应答都接收到了，但是，这些应答传递回来的信息里面，可能是所有的虚拟机创建成功了，可能是部分创建成功。
			if (getVmsRequested() == getVmsAcks()) {//如果被请求创建的虚拟机数等于数据中心反馈回来的ack数。
				// find id of the next datacenter that has not been tried  找到下一个还没有尝试过在其上面创建虚拟机的数据中心。
				for (int nextDatacenterId : getDatacenterIdsList()) {/*迭代出DatacenterIdList列表，存放着所有数据中心ID的列表。
																	  *请细看本类中以下方法的注释：
				                                                      *DatacenterIdList：protected void processResourceCharacteristicsRequest(SimEvent ev) {
				                                                      *setDatacenterIdsList(CloudSim.getCloudResourceList());//设置云资源列表。
				                                                      *...}
				                                                      **/
					//如果下一个数据中心是没有被请求过的，尝试在该数据中心上创建虚拟机。
					if (!getDatacenterRequestedIdsList().contains(nextDatacenterId)) {/*DatacenterRequestedIdsList：
																					   *参见以下方法的注释：
					 																   *protected void processResourceCharacteristics(SimEvent ev) {
					 																   *...
					 																   *setDatacenterRequestedIdsList(new ArrayList<Integer>());
					 																   *...
					 																   *}
					 																   */
						createVmsInDatacenter(nextDatacenterId);
						return;
					}
				}

				// all datacenters already queried  所有的數據中心都已經查詢過。
				if (getVmsCreatedList().size() > 0) { // if some vm were created
					submitCloudlets();//提交云任务。
				} else { // no vms created. abort   没有创建好的虚拟机，终止。
					Log.printLine(CloudSim.clock() + ": " + getName()
							+ ": none of the required VMs could be created. Aborting");
					finishExecution();//发送一个内部事件通信仿真的结束。
				}
			}
		}
	}

	/**
	 * Process a cloudlet return event.
	 * 
	 * 处理一个由数据中心发送回来的事件，这个事件是云任务处理完后发送回来给DatacenterBroker的一个反馈信息。
	 * 
	 * @param ev a SimEvent object
	 * @pre ev != $null
	 * @post $none
	 */
	protected void processCloudletReturn(SimEvent ev) {
		Cloudlet cloudlet = (Cloudlet) ev.getData();//从事件中取出已经完成的云任务。
		getCloudletReceivedList().add(cloudlet);//用一个列表记录被接收的云任务.
		Log.printLine(CloudSim.clock() + ": " + getName() + ": Cloudlet " + cloudlet.getCloudletId()
				+ " received");
		cloudletsSubmitted--;
		//如果等待的云任务列表为0且已经提交的云任务的列表为0。
		if (getCloudletList().size() == 0 && cloudletsSubmitted == 0) { // all cloudlets executed
			Log.printLine(CloudSim.clock() + ": " + getName() + ": All Cloudlets executed. Finishing...");
			clearDatacenters();
			finishExecution();
		} else { // some cloudlets haven't finished yet  如果还有一些云任务还没有完成。
			if (getCloudletList().size() > 0 && cloudletsSubmitted == 0) {//如果云任务的等待列表不为0，已经提交的云任务列表为0；  这两个列表的关系：详见本类protected void submitCloudlets() {..if(vm == null){..continue;..}...}的注释。
				// all the cloudlets sent finished. It means that some bount  所有已经发送的云任务完成。  意味着一些事先绑定好的云任务在等待它的虚拟机被创建。
				// cloudlet is waiting its VM be created
				clearDatacenters();
				createVmsInDatacenter(0);
			}

		}
	}

	/**
	 * Overrides this method when making a new and different type of Broker. This method is called
	 * by {@link #body()} for incoming unknown tags.
	 * 
	 * 当创造出一个新的且不同类型的代理覆写这个方法。这个方法用于连入未知的一些标签时被调用.
	 * 
	 * @param ev a SimEvent object
	 * @pre ev != null
	 * @post $none
	 */
	protected void processOtherEvent(SimEvent ev) {
		if (ev == null) {
			Log.printLine(getName() + ".processOtherEvent(): " + "Error - an event is null.");
			return;
		}

		Log.printLine(getName() + ".processOtherEvent(): "
				+ "Error - event unknown by this DatacenterBroker.");
	}

	/**
	 * Create the virtual machines in a datacenter.
	 * 
	 * @param datacenterId Id of the chosen PowerDatacenter
	 * @pre $none
	 * @post $none
	 */
	protected void createVmsInDatacenter(int datacenterId) {
		// send as much vms as possible for this datacenter before trying the next one 在尝试下一个數據中心之前，这個数据中心发送尽可能多的vm。
		int requestedVms = 0;
		String datacenterName = CloudSim.getEntityName(datacenterId);//通过数据中心的ID得到其相应的名字。 
		for (Vm vm : getVmList()) {//迭代出DatacenterBroker在启动CloudSim核心模拟引擎时，事先配置好的vmList(需要被创建的虚拟机).
			if (!getVmsToDatacentersMap().containsKey(vm.getId())) {//如果虛擬機是一台創建不成功的虛擬機。  從存放了創建成功的虛擬機ID與對應的數據中心ID的Map集合中，查看是否有這臺虛擬機，如果沒有，顯然是創建不成功的虛擬機。要對其採取相應的動作。
				Log.printLine(CloudSim.clock() + ": " + getName() + ": Trying to Create VM #" + vm.getId()
						+ " in " + datacenterName);
				sendNow(datacenterId, CloudSimTags.VM_CREATE_ACK, vm);//不断地發送一個創建虛擬機的請求給Datacenter.这里共有getVmList().size()个事件被放入future队列，等待下一次while大循环下的runClockTick()将他们从future队列放入到deffered队列里面，等待多一次while大循环，迭代出实体去处理属于自己处理的事件。
				requestedVms++;//請求的虛擬機累加。統計被請求創建的虛擬機數。
			}
		}

		getDatacenterRequestedIdsList().add(datacenterId);//当上面向数据中心发送了一个创建虚拟机请求后，要记录下被请求的数据中心。

		setVmsRequested(requestedVms);//更新统计被请求创建的虚拟机数。
		setVmsAcks(0);//将一个新回应重置为0；
	}

	/**
	 * Submit cloudlets to the created VMs.
	 * 提交云任务给已经创建好的虚拟机。
	 * 
	 * @pre $none
	 * @post $none
	 */
	protected void submitCloudlets() {
		int vmIndex = 0;
		for (Cloudlet cloudlet : getCloudletList()) {//迭代出DatacenterBroker在启动CloudSim核心模拟引擎时，事先配置好的cloudletList(需要被提交的云任务)。请参照example1.java
			Vm vm;
			// if user didn't bind this cloudlet and it has not been executed yet  如果用户还没有绑定这个任务并且该任务还没有被执行。
			if (cloudlet.getVmId() == -1) {//如果该任务所绑定的虚拟机是初始化状态，即未绑定任何虚拟机。 说明：id 0、1、2、3...。-1表示为空的状态。
				vm = getVmsCreatedList().get(vmIndex);//先从创建就好的虚拟机列表取出vmIndex位置的虚拟机。
			} else { // submit to the specific vm  //如果该任务不是刚刚初始化的，而是指定提交到特定的虚拟机上的。       参见：public void bindCloudletToVm(int cloudletId, int vmId) {...}  说明：这些在等待列表中提前绑定好虚拟机的云任务是在启动仿真前就已经配置好的。见example7.java
				vm = VmList.getById(getVmsCreatedList(), cloudlet.getVmId());//已经创建好的虚拟机列表中，取出该云任务所绑定的虚拟机。
				if (vm == null) { // vm was not created  如果虚拟机不是已经创建已经好的。
					Log.printLine(CloudSim.clock() + ": " + getName() + ": Postponing execution of cloudlet "
							+ cloudlet.getCloudletId() + ": bount VM not available");//打印：xx：DatacenterBroker1：推迟执行那些绑定了不可用的虚拟机的云任务。
					continue;//中断本次循环。
				}
			}
			//打印：xx时刻:DatacenterBroker：发送云任务id到虚拟机#id
			Log.printLine(CloudSim.clock() + ": " + getName() + ": Sending cloudlet "
					+ cloudlet.getCloudletId() + " to VM #" + vm.getId());
			cloudlet.setVmId(vm.getId());//云任务与虚拟机建立联系，即绑定在一起。
			sendNow(getVmsToDatacentersMap().get(vm.getId()), CloudSimTags.CLOUDLET_SUBMIT, cloudlet);/*发送提交任务到虚拟机的事件到future队列，future队列再到deffered队列，等待数据中心处理该事件。
			 																						   *
			 																						   *发送云任务到指定的数据中心。云任务与对应的虚拟机已经建立好联系。
			 																						   *
			 																						   *参数说明：
			 																						   *1.getVmsToDatacentersMap().get(vm.getId())；
			 																						   *详细注释参见：protected void processVmCreate(SimEvent ev) {...}
			 																						   *取出创建成功的虚拟机对应的数据中心。
			 																						   *
			 																						   *2.标签：CloudSimTags.CLOUDLET_SUBMIT
			 																						   *用来匹配事件。
			 																						   *
			 																						   *与predicate的区别是：
			 																						   *tag:用来匹配某一类事件。
			 																						   *predicate:是用来选择、指定特定的一个事件。具有较大的灵活性，特定的事件具有优先处理的权。
			 																						   *
			 																						   *关系：一般是predicate先筛选出了特定事件后，在到tag来匹配事件的类型，才开始采取相应的动作。
			 																						   *
			 																						   *3.云任务。
			 																						   */
			cloudletsSubmitted++;//统计提交的云任务数。
			//可修改的任务与虚拟机的绑定策略。
			vmIndex = (vmIndex + 1) % getVmsCreatedList().size();//计算下一个索引位置。
			getCloudletSubmittedList().add(cloudlet);//将已经提交的云任务记录在cloudletSubmittedList中。
		}

		// remove submitted cloudlets from waiting list 从等待列表CloudletList(启动仿真前配置好的)中移除那些已经提交到虚拟机上的云任务。
		for (Cloudlet cloudlet : getCloudletSubmittedList()) {
			getCloudletList().remove(cloudlet);
		}
	}

	/**
	 * Destroy the virtual machines running in datacenters.
	 * 
	 * @pre $none
	 * @post $none
	 */
	protected void clearDatacenters() {
		for (Vm vm : getVmsCreatedList()) {
			Log.printLine(CloudSim.clock() + ": " + getName() + ": Destroying VM #" + vm.getId());
			sendNow(getVmsToDatacentersMap().get(vm.getId()), CloudSimTags.VM_DESTROY, vm);
		}

		getVmsCreatedList().clear();
	}

	/**
	 * Send an internal event communicating the end of the simulation.
	 * 
	 * @pre $none
	 * @post $none
	 */
	protected void finishExecution() {
		sendNow(getId(), CloudSimTags.END_OF_SIMULATION);
	}

	/*
	 * (non-Javadoc)
	 * @see cloudsim.core.SimEntity#shutdownEntity()
	 */
	@Override
	public void shutdownEntity() {
		Log.printLine(getName() + " is shutting down...");
	}

	/*
	 * (non-Javadoc)
	 * @see cloudsim.core.SimEntity#startEntity()
	 */
	@Override
	public void startEntity() {
		Log.printLine(getName() + " is starting...");
		schedule(getId(), 0, CloudSimTags.RESOURCE_CHARACTERISTICS_REQUEST);
	}

	/**
	 * Gets the vm list.
	 * 
	 * @param <T> the generic type
	 * @return the vm list
	 */
	@SuppressWarnings("unchecked")
	public <T extends Vm> List<T> getVmList() {
		return (List<T>) vmList;
	}

	/**
	 * Sets the vm list.
	 * 
	 * @param <T> the generic type
	 * @param vmList the new vm list
	 */
	protected <T extends Vm> void setVmList(List<T> vmList) {
		this.vmList = vmList;
	}

	/**
	 * Gets the cloudlet list.
	 * 
	 * @param <T> the generic type
	 * @return the cloudlet list
	 */
	@SuppressWarnings("unchecked")
	public <T extends Cloudlet> List<T> getCloudletList() {
		return (List<T>) cloudletList;
	}

	/**
	 * Sets the cloudlet list.
	 * 
	 * @param <T> the generic type
	 * @param cloudletList the new cloudlet list
	 */
	protected <T extends Cloudlet> void setCloudletList(List<T> cloudletList) {
		this.cloudletList = cloudletList;
	}

	/**
	 * Gets the cloudlet submitted list.
	 * 
	 * @param <T> the generic type
	 * @return the cloudlet submitted list
	 */
	@SuppressWarnings("unchecked")
	public <T extends Cloudlet> List<T> getCloudletSubmittedList() {
		return (List<T>) cloudletSubmittedList;
	}

	/**
	 * Sets the cloudlet submitted list.
	 * 
	 * @param <T> the generic type
	 * @param cloudletSubmittedList the new cloudlet submitted list
	 */
	protected <T extends Cloudlet> void setCloudletSubmittedList(List<T> cloudletSubmittedList) {
		this.cloudletSubmittedList = cloudletSubmittedList;
	}

	/**
	 * Gets the cloudlet received list.
	 * 
	 * @param <T> the generic type
	 * @return the cloudlet received list
	 */
	@SuppressWarnings("unchecked")
	public <T extends Cloudlet> List<T> getCloudletReceivedList() {
		return (List<T>) cloudletReceivedList;
	}

	/**
	 * Sets the cloudlet received list.
	 * 
	 * @param <T> the generic type
	 * @param cloudletReceivedList the new cloudlet received list
	 */
	protected <T extends Cloudlet> void setCloudletReceivedList(List<T> cloudletReceivedList) {
		this.cloudletReceivedList = cloudletReceivedList;
	}

	/**
	 * Gets the vm list.
	 * 
	 * @param <T> the generic type
	 * @return the vm list
	 */
	@SuppressWarnings("unchecked")
	public <T extends Vm> List<T> getVmsCreatedList() {
		return (List<T>) vmsCreatedList;
	}

	/**
	 * Sets the vm list.
	 * 
	 * @param <T> the generic type
	 * @param vmsCreatedList the vms created list
	 */
	protected <T extends Vm> void setVmsCreatedList(List<T> vmsCreatedList) {
		this.vmsCreatedList = vmsCreatedList;
	}

	/**
	 * Gets the vms requested.
	 * 
	 * @return the vms requested
	 */
	protected int getVmsRequested() {
		return vmsRequested;
	}

	/**
	 * Sets the vms requested.
	 * 
	 * @param vmsRequested the new vms requested
	 */
	protected void setVmsRequested(int vmsRequested) {
		this.vmsRequested = vmsRequested;
	}

	/**
	 * Gets the vms acks.
	 * 
	 * @return the vms acks
	 */
	protected int getVmsAcks() {
		return vmsAcks;
	}

	/**
	 * Sets the vms acks.
	 * 
	 * @param vmsAcks the new vms acks
	 */
	protected void setVmsAcks(int vmsAcks) {
		this.vmsAcks = vmsAcks;
	}

	/**
	 * Increment vms acks.
	 */
	protected void incrementVmsAcks() {
		vmsAcks++;
	}

	/**
	 * Gets the vms destroyed.
	 * 
	 * @return the vms destroyed
	 */
	protected int getVmsDestroyed() {
		return vmsDestroyed;
	}

	/**
	 * Sets the vms destroyed.
	 * 
	 * @param vmsDestroyed the new vms destroyed
	 */
	protected void setVmsDestroyed(int vmsDestroyed) {
		this.vmsDestroyed = vmsDestroyed;
	}

	/**
	 * Gets the datacenter ids list.
	 * 
	 * @return the datacenter ids list
	 */
	protected List<Integer> getDatacenterIdsList() {
		return datacenterIdsList;
	}

	/**
	 * Sets the datacenter ids list.
	 * 
	 * @param datacenterIdsList the new datacenter ids list
	 */
	protected void setDatacenterIdsList(List<Integer> datacenterIdsList) {
		this.datacenterIdsList = datacenterIdsList;
	}

	/**
	 * Gets the vms to datacenters map.
	 * 
	 * @return the vms to datacenters map
	 */
	protected Map<Integer, Integer> getVmsToDatacentersMap() {
		return vmsToDatacentersMap;
	}

	/**
	 * Sets the vms to datacenters map.
	 * 
	 * @param vmsToDatacentersMap the vms to datacenters map
	 */
	protected void setVmsToDatacentersMap(Map<Integer, Integer> vmsToDatacentersMap) {
		this.vmsToDatacentersMap = vmsToDatacentersMap;
	}

	/**
	 * Gets the datacenter characteristics list.
	 * 
	 * @return the datacenter characteristics list
	 */
	protected Map<Integer, DatacenterCharacteristics> getDatacenterCharacteristicsList() {
		return datacenterCharacteristicsList;
	}

	/**
	 * Sets the datacenter characteristics list.
	 * 
	 * @param datacenterCharacteristicsList the datacenter characteristics list
	 */
	protected void setDatacenterCharacteristicsList(
			Map<Integer, DatacenterCharacteristics> datacenterCharacteristicsList) {
		this.datacenterCharacteristicsList = datacenterCharacteristicsList;
	}

	/**
	 * Gets the datacenter requested ids list.
	 * 
	 * @return the datacenter requested ids list
	 */
	protected List<Integer> getDatacenterRequestedIdsList() {
		return datacenterRequestedIdsList;
	}

	/**
	 * Sets the datacenter requested ids list.
	 * 
	 * @param datacenterRequestedIdsList the new datacenter requested ids list
	 */
	protected void setDatacenterRequestedIdsList(List<Integer> datacenterRequestedIdsList) {
		this.datacenterRequestedIdsList = datacenterRequestedIdsList;
	}

}
