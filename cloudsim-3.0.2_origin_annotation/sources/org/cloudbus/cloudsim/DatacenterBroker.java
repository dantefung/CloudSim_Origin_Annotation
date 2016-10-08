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
	 * ָ��һ�����������������������һ��ָ����������ϡ�
	 * 
	 * @param cloudletId ID of the cloudlet being bount to a vm
	 * @param vmId the vm id
	 * @pre cloudletId > 0
	 * @pre id > 0
	 * @post $none
	 */
	public void bindCloudletToVm(int cloudletId, int vmId) {
		CloudletList.getById(getCloudletList(), cloudletId).setVmId(vmId);//��������ĵȴ��б���ȡ��ָ��������Ȼ�󽫸��������ض�����������а󶨡�
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
				processResourceCharacteristicsRequest(ev);//�����Լ����͸��Լ���һ����Ϣ��
				break;
			// Resource characteristics answer
			case CloudSimTags.RESOURCE_CHARACTERISTICS:
				processResourceCharacteristics(ev);//̎�픵�����Č�DatacenterBrokerՈ�󔵓����ĵ�һЩ���A�Oʩ��Ո�󣬰l�ͻ؁��ޒ���¼���
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
	 * DatacenterBroker����(����������������Ӳ����Դ�������������)��Ӧ��
	 * 
	 * ���ò㼶��ϵ��
	 * ����һ��DatacenterBroker��DatacenterBroker������ʱ��ͨ���Լ����캯�������ø���Ĺ��캯��������name��ͬʱ�����Լ��Ķ�������CloudSim�е�entities�б��С�
	 * CloudSim.java(
	 * startSimulation();
	 * -->run()
	 * -->runStart()����entity�б��е�����ʵ�壬���������
	 * -->startEntity()����DatacenterBroker���ԣ�schedule(getId(), 0, CloudSimTags.RESOURCE_CHARACTERISTICS_REQUEST);�����Ϣ�ᱻ��װ��һ���¼�������future���У��ٽ���future���е���deffered���У���ʱ���Լ����մ���protected void processResourceCharacteristicsRequest(SimEvent ev) {...}��
	 * -->runClockTick()
	 * -->��entity�б�ȡ����ent����PowerDatacerBroker����:ent.run();
	 * -->����ʱ������ȡ����ʵ���ܴ�����¼���
	 * -->���࣬PowerDatacenterBroker����DatacenterBroker���󣩵�processEvent(ev)������
	 * -->case CloudSimTags.RESOURCE_CHARACTERISTICS:processResourceCharacteristics(ev);
	 * 
	 * 
	 * @param ev a SimEvent object
	 * @pre ev != $null
	 * @post $none
	 */
	 protected void processResourceCharacteristics(SimEvent ev) {
		DatacenterCharacteristics characteristics = (DatacenterCharacteristics) ev.getData();//���������ķ��ͻ����Ļ�Ӧ�¼���ȡ�����������ĵĻ���Ӳ����Դ����
		getDatacenterCharacteristicsList().put(characteristics.getId(), characteristics);//���������ĵĻ���Ӳ����Դ(������������)�������map���ϡ�

		if (getDatacenterCharacteristicsList().size() == getDatacenterIdsList().size()) {//��CloudSim.java(runClockTick()��������ÿ��ʵ��-->ent.run()��while{..}���ϵش�deffered queue��ȡ���¼����д���-->processResourceCharacteristics����)
			setDatacenterRequestedIdsList(new ArrayList<Integer>());//����һ���б���������һ���������������Ĵ���������󣬼�¼�Ѿ�����������������ġ�/*ʵ�ִ�����һ������������������б�createVmsInDatacenter(getDatacenterIdsList().get(0))�����õ����б�*/
			createVmsInDatacenter(getDatacenterIdsList().get(0));//�ڸ����������ﴴ���������
		}
	}

	/**
	 * Process a request for the characteristics of a PowerDatacenter.
	 * 
	 *	�Լ������Լ�������ʱ���͵�һ����Ϣ��
	 * 
	 * @param ev a SimEvent object
	 * @pre ev != $null
	 * @post $none
	 */
	protected void processResourceCharacteristicsRequest(SimEvent ev) {
		setDatacenterIdsList(CloudSim.getCloudResourceList());//��������Դ�б�
		                                                      /*	���ò㼶��ϵ��
		                                                       * init()��CloudSim��ʼ����...����CIS����
		                                                       * -->startSimulation()-->run()
		                                                       * -->runStart()-->startEntity()��ʵ��CloudSimShutDown��CloudInformationService��Datacenter��DatacenterBroker���������Կ�ʼ�����¼���
		                                                       * ���� Datacenter:sendNow(gisID, CloudSimTags.REGISTER_RESOURCE, getId());
		                                                       * -->runClockTick()
		                                                       * -->��entity�б�ȡ����ent����CloudInformationService����:ent.run();
		                                                       * -->����ʱ������ȡ����ʵ���ܴ�����¼���
		                                                       * -->���࣬CloudInformationService�����processEvent(ev)������
		                                                       * -->case CloudSimTags.REGISTER_RESOURCE:resList.add((Integer) ev.getData()); ��ʱ��Datacenter�������������Ϣ�������ĵ���Դ�б��
		                                                       * -->...
		                                                       * -->CloudSim.getCloudResourceList();
		                                                       * -->return cis.getList();
		                                                       * -->��cis.getList��ֵ��datacenterIdsList.
		                                                       * */
		setDatacenterCharacteristicsList(new HashMap<Integer, DatacenterCharacteristics>());//���ȴ�����һ��map���ϣ�protected void processResourceCharacteristics(SimEvent ev) {..}��������Ĵ���齫���õ������������ĵĻ���Ӳ����Դ(������������)�������map���ϡ�

		Log.printLine(CloudSim.clock() + ": " + getName() + ": Cloud Resource List received with "
				+ getDatacenterIdsList().size() + " resource(s)");//��ӡ����xxʱ�̣�DatacenterBroker1������Դ�б���յ�xx��Դ��

		for (Integer datacenterId : getDatacenterIdsList()) {//�ӿ��õ����������б��е��������е��������ĵ�ID
			sendNow(datacenterId, CloudSimTags.RESOURCE_CHARACTERISTICS, getId());//�����п��õ��������ķ���һ����Ϣ�������ȡ���������ĵĻ���Ӳ����Դ��
		}//��ʱ��future�����У���getDatacenterIdsList().size()����DatacenterBroker�������¼�.CloudSim�е�run(){.. while(true){.. runClockTick(){..for(){ent.run( while(){processingEvent()��//����ʵ���deffered����������Ӧ����������¼���ֱ���¼���������֮�󣬲��ֵ���һ��ʵ���deffered������Ѱ���Լ�Ҫ����������¼���һ��һ�������ꡣ})} .. }..}.. }����while��ѭ�����������£�1.������֮ǰdeffered����������¼���2.��future����������¼�����deffered�������档����һ�δ�while����ѭ��ʱ��Datacenter��һ��һ���ؽ�deffered�����е����Լ���������¼��������ꡣ
	}

	/**
	 * Process the ack received due to a request for VM creation.
	 * �����Ѿ����յ�����������һ��������������������£���������ack����Ӧ����Ӧ��Ӧ��.
	 * 
	 * ʵ�ʺ��壺
	 * 	DatacenterBroker����һ������Vm�������Datacenter��Datacenter���յ�������󣬿�ʼ�������ͻ�һ��Ӧ��ack����DatacenterBroker��������Ƿ񴴽��ɹ���
	 * �����processVmCreate��������DatacenterBroker�յ���Datacenter����ack��������Ϣ��Ȼ��ʼ���д���������Ӧ�Ķ�����Ӧ����
	 * 
	 * be due to <=> be caused by
	 * 
	 *  ����due to ��Ϊthe ack�ĺ��ö��
	 * 
	 * @param ev a SimEvent object
	 * @pre ev != null
	 * @post $none
	 */
	protected void processVmCreate(SimEvent ev) {
		int[] data = (int[]) ev.getData();//Object data,�洢��������ݣ������Ǵ洢��һ�����顣
		int datacenterId = data[0];//�������ĵ�ID
		int vmId = data[1];//�ڔ������Ą������������ID
		int result = data[2];//����̓�M�C�Ƿ�ɹ���һ���Y����

		/**
		 * New annotation by DanteFung.
		 * 
		 * �@һ���Ǚz�Ո�󄓽��ľ��wĳ̨̓�M�C�Ƿ��ڔ������Ą����ɹ��ˡ�
		 * �ǡ�1.��vmsCreatedList�б�ӛ����Є����õ�̓�M�C  2.map���ϣ�ӛ�̓�M�CID�͔�������ID��һ��ӳ�䡣
		 * �������̨ݔ�����w���_̓�M�C�ھ��w���Ă��������Ą���ʧ����
		 * 
		 */
		if (result == CloudSimTags.TRUE) {//���̓�M�C�����ɹ�
			getVmsToDatacentersMap().put(vmId, datacenterId);//̓�M�C�ڔ������ăȄ����ɹ�����һ��map����ӛ䛣�̓�M�CID�͔������ĵ�һ��ӳ���P�S����̓�M�C�c�䌦���Ĕ��������P����
			getVmsCreatedList().add(VmList.getById(getVmList(), vmId));//�������ɹ���̓�M�Cӛ���VmsCreatedList�б��ԓ�б���ֱ�ӽ���һ��Vm����
			Log.printLine(CloudSim.clock() + ": " + getName() + ": VM #" + vmId
					+ " has been created in Datacenter #" + datacenterId + ", Host #"
					+ VmList.getById(getVmsCreatedList(), vmId).getHost().getId());//����vmID��vmsCreatedList�б���ȡ��������Vm����==>���õ������ɹ���Vm����==>ȡ��vm��������̨���CHost��ID. ��UML����⣬vm��host����P�P�S��
		} else {//����������ɹ��������_ݔ�������w��̨̓�M�C���Ă��������ăȄ���ʧ����
			Log.printLine(CloudSim.clock() + ": " + getName() + ": Creation of VM #" + vmId
					+ " failed in Datacenter #" + datacenterId);
		}

		incrementVmsAcks();//�yӋ�õ���ޒ�������@��һ���������^�̡�
		
		/**
		 *	����Ĵ��a�K����ᘌ���̨̓�M�C���Եĵ���ӛ��^�̡���vmList�ȵ�����̓�M�C���ԣ��oՓ���ڔ������Ą����ɹ��c�񣬶����Ĕ�������̎�l�ͻ�һ��������Ϣ�؁�oDatacenterBroker�� 
		 * 
		 * 	���µĴ��a�K������    ��������Є����ɹ���̓�M�C���б��L��  == ��ű�Ո�󄓽�������̓�M�C���б� - �N�S����̓�M�C�� ��������r�²ň��С�
		 * 	vmsDestroyed����DatacenterBroker��������r��ͨ�^���캯����ʼ����0��
		 * 
		 * 	��ζ����DatacenterBroker��Ո���Ҫ����������̓�M�C��Datacenterȫ���������ɹ��ˡ�
		 * 
		 * 	�_ʼ��������Ĵ��a�K��
		 * 	
		 */

		// all the requested VMs have been created ���б�Ҫ���̓�M�C�ѽ������ɹ���
		if (getVmsCreatedList().size() == getVmList().size() - getVmsDestroyed()) {
			submitCloudlets();//�ύ�������Ѿ������õ�̓�M�C�ϡ�
		} else {//����������е����󴴽���������������ã����в������������ʧ�ܵĻ����ͳ���������һ�����������ϴ����������
			// all the acks received, but some VMs were not created  Datacenter�ϴ�����ÿһ������������۴����ɹ���񣬶��᷵��һ��Ӧ����ˣ����е�Ӧ�𶼽��յ��ˣ����ǣ���ЩӦ�𴫵ݻ�������Ϣ���棬���������е�����������ɹ��ˣ������ǲ��ִ����ɹ���
			if (getVmsRequested() == getVmsAcks()) {//��������󴴽���������������������ķ���������ack����
				// find id of the next datacenter that has not been tried  �ҵ���һ����û�г��Թ��������洴����������������ġ�
				for (int nextDatacenterId : getDatacenterIdsList()) {/*������DatacenterIdList�б������������������ID���б�
																	  *��ϸ�����������·�����ע�ͣ�
				                                                      *DatacenterIdList��protected void processResourceCharacteristicsRequest(SimEvent ev) {
				                                                      *setDatacenterIdsList(CloudSim.getCloudResourceList());//��������Դ�б�
				                                                      *...}
				                                                      **/
					//�����һ������������û�б�������ģ������ڸ����������ϴ����������
					if (!getDatacenterRequestedIdsList().contains(nextDatacenterId)) {/*DatacenterRequestedIdsList��
																					   *�μ����·�����ע�ͣ�
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

				// all datacenters already queried  ���еĔ������Ķ��ѽ���ԃ�^��
				if (getVmsCreatedList().size() > 0) { // if some vm were created
					submitCloudlets();//�ύ������
				} else { // no vms created. abort   û�д����õ����������ֹ��
					Log.printLine(CloudSim.clock() + ": " + getName()
							+ ": none of the required VMs could be created. Aborting");
					finishExecution();//����һ���ڲ��¼�ͨ�ŷ���Ľ�����
				}
			}
		}
	}

	/**
	 * Process a cloudlet return event.
	 * 
	 * ����һ�����������ķ��ͻ������¼�������¼���������������ͻ�����DatacenterBroker��һ��������Ϣ��
	 * 
	 * @param ev a SimEvent object
	 * @pre ev != $null
	 * @post $none
	 */
	protected void processCloudletReturn(SimEvent ev) {
		Cloudlet cloudlet = (Cloudlet) ev.getData();//���¼���ȡ���Ѿ���ɵ�������
		getCloudletReceivedList().add(cloudlet);//��һ���б��¼�����յ�������.
		Log.printLine(CloudSim.clock() + ": " + getName() + ": Cloudlet " + cloudlet.getCloudletId()
				+ " received");
		cloudletsSubmitted--;
		//����ȴ����������б�Ϊ0���Ѿ��ύ����������б�Ϊ0��
		if (getCloudletList().size() == 0 && cloudletsSubmitted == 0) { // all cloudlets executed
			Log.printLine(CloudSim.clock() + ": " + getName() + ": All Cloudlets executed. Finishing...");
			clearDatacenters();
			finishExecution();
		} else { // some cloudlets haven't finished yet  �������һЩ������û����ɡ�
			if (getCloudletList().size() > 0 && cloudletsSubmitted == 0) {//���������ĵȴ��б�Ϊ0���Ѿ��ύ���������б�Ϊ0��  �������б�Ĺ�ϵ���������protected void submitCloudlets() {..if(vm == null){..continue;..}...}��ע�͡�
				// all the cloudlets sent finished. It means that some bount  �����Ѿ����͵���������ɡ�  ��ζ��һЩ���Ȱ󶨺õ��������ڵȴ������������������
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
	 * �������һ���µ��Ҳ�ͬ���͵Ĵ���д������������������������δ֪��һЩ��ǩʱ������.
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
		// send as much vms as possible for this datacenter before trying the next one �ڳ�����һ����������֮ǰ���₀�������ķ��;����ܶ��vm��
		int requestedVms = 0;
		String datacenterName = CloudSim.getEntityName(datacenterId);//ͨ���������ĵ�ID�õ�����Ӧ�����֡� 
		for (Vm vm : getVmList()) {//������DatacenterBroker������CloudSim����ģ������ʱ���������úõ�vmList(��Ҫ�������������).
			if (!getVmsToDatacentersMap().containsKey(vm.getId())) {//���̓�M�C��һ̨�������ɹ���̓�M�C��  �Ĵ���˄����ɹ���̓�M�CID�c�����Ĕ�������ID��Map�����У��鿴�Ƿ����@�_̓�M�C������]�У��@Ȼ�Ǆ������ɹ���̓�M�C��Ҫ�����ȡ�����Ą�����
				Log.printLine(CloudSim.clock() + ": " + getName() + ": Trying to Create VM #" + vm.getId()
						+ " in " + datacenterName);
				sendNow(datacenterId, CloudSimTags.VM_CREATE_ACK, vm);//���ϵذl��һ������̓�M�C��Ո��oDatacenter.���ﹲ��getVmList().size()���¼�������future���У��ȴ���һ��while��ѭ���µ�runClockTick()�����Ǵ�future���з��뵽deffered�������棬�ȴ���һ��while��ѭ����������ʵ��ȥ���������Լ�������¼���
				requestedVms++;//Ո���̓�M�C�ۼӡ��yӋ��Ո�󄓽���̓�M�C����
			}
		}

		getDatacenterRequestedIdsList().add(datacenterId);//���������������ķ�����һ����������������Ҫ��¼�±�������������ġ�

		setVmsRequested(requestedVms);//����ͳ�Ʊ����󴴽������������
		setVmsAcks(0);//��һ���»�Ӧ����Ϊ0��
	}

	/**
	 * Submit cloudlets to the created VMs.
	 * �ύ��������Ѿ������õ��������
	 * 
	 * @pre $none
	 * @post $none
	 */
	protected void submitCloudlets() {
		int vmIndex = 0;
		for (Cloudlet cloudlet : getCloudletList()) {//������DatacenterBroker������CloudSim����ģ������ʱ���������úõ�cloudletList(��Ҫ���ύ��������)�������example1.java
			Vm vm;
			// if user didn't bind this cloudlet and it has not been executed yet  ����û���û�а���������Ҹ�����û�б�ִ�С�
			if (cloudlet.getVmId() == -1) {//������������󶨵�������ǳ�ʼ��״̬����δ���κ�������� ˵����id 0��1��2��3...��-1��ʾΪ�յ�״̬��
				vm = getVmsCreatedList().get(vmIndex);//�ȴӴ����ͺõ�������б�ȡ��vmIndexλ�õ��������
			} else { // submit to the specific vm  //����������Ǹոճ�ʼ���ģ�����ָ���ύ���ض���������ϵġ�       �μ���public void bindCloudletToVm(int cloudletId, int vmId) {...}  ˵������Щ�ڵȴ��б�����ǰ�󶨺��������������������������ǰ���Ѿ����úõġ���example7.java
				vm = VmList.getById(getVmsCreatedList(), cloudlet.getVmId());//�Ѿ������õ�������б��У�ȡ�������������󶨵��������
				if (vm == null) { // vm was not created  �������������Ѿ������Ѿ��õġ�
					Log.printLine(CloudSim.clock() + ": " + getName() + ": Postponing execution of cloudlet "
							+ cloudlet.getCloudletId() + ": bount VM not available");//��ӡ��xx��DatacenterBroker1���Ƴ�ִ����Щ���˲����õ��������������
					continue;//�жϱ���ѭ����
				}
			}
			//��ӡ��xxʱ��:DatacenterBroker������������id�������#id
			Log.printLine(CloudSim.clock() + ": " + getName() + ": Sending cloudlet "
					+ cloudlet.getCloudletId() + " to VM #" + vm.getId());
			cloudlet.setVmId(vm.getId());//�������������������ϵ��������һ��
			sendNow(getVmsToDatacentersMap().get(vm.getId()), CloudSimTags.CLOUDLET_SUBMIT, cloudlet);/*�����ύ������������¼���future���У�future�����ٵ�deffered���У��ȴ��������Ĵ�����¼���
			 																						   *
			 																						   *����������ָ�����������ġ����������Ӧ��������Ѿ���������ϵ��
			 																						   *
			 																						   *����˵����
			 																						   *1.getVmsToDatacentersMap().get(vm.getId())��
			 																						   *��ϸע�Ͳμ���protected void processVmCreate(SimEvent ev) {...}
			 																						   *ȡ�������ɹ����������Ӧ���������ġ�
			 																						   *
			 																						   *2.��ǩ��CloudSimTags.CLOUDLET_SUBMIT
			 																						   *����ƥ���¼���
			 																						   *
			 																						   *��predicate�������ǣ�
			 																						   *tag:����ƥ��ĳһ���¼���
			 																						   *predicate:������ѡ��ָ���ض���һ���¼������нϴ������ԣ��ض����¼��������ȴ����Ȩ��
			 																						   *
			 																						   *��ϵ��һ����predicate��ɸѡ�����ض��¼����ڵ�tag��ƥ���¼������ͣ��ſ�ʼ��ȡ��Ӧ�Ķ�����
			 																						   *
			 																						   *3.������
			 																						   */
			cloudletsSubmitted++;//ͳ���ύ������������
			//���޸ĵ�������������İ󶨲��ԡ�
			vmIndex = (vmIndex + 1) % getVmsCreatedList().size();//������һ������λ�á�
			getCloudletSubmittedList().add(cloudlet);//���Ѿ��ύ���������¼��cloudletSubmittedList�С�
		}

		// remove submitted cloudlets from waiting list �ӵȴ��б�CloudletList(��������ǰ���úõ�)���Ƴ���Щ�Ѿ��ύ��������ϵ�������
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
