/*
 * Title:        CloudSim Toolkit
 * Description:  CloudSim (Cloud Simulation) Toolkit for Modeling and Simulation of Clouds
 * Licence:      GPL - http://www.gnu.org/copyleft/gpl.html
 *
 * Copyright (c) 2009-2012, The University of Melbourne, Australia
 */

package org.cloudbus.cloudsim.power;

import java.util.List;
import java.util.Map;

import org.cloudbus.cloudsim.Datacenter;
import org.cloudbus.cloudsim.DatacenterCharacteristics;
import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.Storage;
import org.cloudbus.cloudsim.Vm;
import org.cloudbus.cloudsim.VmAllocationPolicy;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.core.CloudSimTags;
import org.cloudbus.cloudsim.core.SimEvent;
import org.cloudbus.cloudsim.core.predicates.PredicateType;

/**
 * PowerDatacenter is a class that enables simulation of power-aware data centers.
 * PowerDatacenter ��һ�������ܺļ�ط����������ĵ��ࡣ
 * 
 * If you are using any algorithms, policies or workload included in the power package please cite
 * the following paper:
 * ����㽫ʹ��������㷨�����Ի��߰�����Power���¹������أ����������µ����ģ�
 * Anton Beloglazov, and Rajkumar Buyya, "Optimal Online Deterministic Algorithms and Adaptive
 * Heuristics for Energy and Performance Efficient Dynamic Consolidation of Virtual Machines in
 * Cloud Data Centers", Concurrency and Computation: Practice and Experience (CCPE), Volume 24,
 * Issue 13, Pages: 1397-1420, John Wiley & Sons, Ltd, New York, USA, 2012
 * 
 * @author Anton Beloglazov
 * @since CloudSim Toolkit 2.0
 */
public class PowerDatacenter extends Datacenter {

	/** The power. */
	private double power;

	/** The disable migrations. */
	private boolean disableMigrations;

	/** The cloudlet submited. */
	private double cloudletSubmitted;

	/** The migration count. */
	private int migrationCount;

	/**
	 * Instantiates a new datacenter. ʵ����һ���µ���������
	 * 
	 * @param name the name
	 * @param characteristics the res config
	 * @param schedulingInterval the scheduling interval�������
	 * @param utilizationBound the utilization bound
	 * @param vmAllocationPolicy the vm provisioner
	 * @param storageList the storage list
	 * @throws Exception the exception
	 */
	public PowerDatacenter(
			String name,
			DatacenterCharacteristics characteristics,
			VmAllocationPolicy vmAllocationPolicy,
			List<Storage> storageList,
			double schedulingInterval) throws Exception {
		super(name, characteristics, vmAllocationPolicy, storageList, schedulingInterval); 

		setPower(0.0);
		setDisableMigrations(false);
		setCloudletSubmitted(-1);
		setMigrationCount(0);
	}

	/**
	 * Updates processing of each cloudlet running in this PowerDatacenter. It is necessary because
	 * Hosts and VirtualMachines are simple objects, not entities. So, they don't receive events and
	 * updating cloudlets inside them must be called from the outside.
	 * 
	 * @pre $none
	 * @post $none
	 */
	@Override
	protected void updateCloudletProcessing() {
		if (getCloudletSubmitted() == -1 || getCloudletSubmitted() == CloudSim.clock()) {
			CloudSim.cancelAll(getId(), new PredicateType(CloudSimTags.VM_DATACENTER_EVENT));
			schedule(getId(), getSchedulingInterval(), CloudSimTags.VM_DATACENTER_EVENT);
			return;
		}
		double currentTime = CloudSim.clock();

		// if some time passed since last processing
		if (currentTime > getLastProcessTime()) {
			System.out.print(currentTime + " ");

			double minTime = updateCloudetProcessingWithoutSchedulingFutureEventsForce();

			/*
			 * �������Ч��Ǩ�ƵĻ�.
			 * �����µĲ�����
			 * �������������Ǩ�Ʒ��������ȡ��һ���洢��Map<String, Object>���͵����ݵ��б�
			 * 
			 * �ж����б�Ϊ�յ�����£�������ÿ��map����,�Ӽ����зֱ�ȡ��VM��Host��
			 * ��Vm��ȡ�������Լ����󶨵�����host.
			 * 
			 * targetHost��map������ȡ�������������Host��
			 * oldHost��vm�󶨵�����Host��
			 * 
			 * ��Vmû�а��κ�������ֱ�ӽ���������󶨵�targetHost�����Ŀ��������
			 * ��Vm���Ѿ�����һ̨�������򽫸�vm��oldHostǨ�Ƶ�targetHost�ϡ�targetHost.addMigratingInVm(vm);
			 * 
			 * ͳ��Ǩ�Ƶ��������Ŀ��
			 * δ����������List<Map<String, Object>> migrationMap = getVmAllocationPolicy().optimizeAllocation(getVmList());
			 * */
			if (!isDisableMigrations()) {
				List<Map<String, Object>> migrationMap = getVmAllocationPolicy().optimizeAllocation(
						getVmList());

				if (migrationMap != null) {
					for (Map<String, Object> migrate : migrationMap) {
						Vm vm = (Vm) migrate.get("vm");
						PowerHost targetHost = (PowerHost) migrate.get("host");
						PowerHost oldHost = (PowerHost) vm.getHost();

						if (oldHost == null) {
							Log.formatLine(
									"%.2f: Migration of VM #%d to Host #%d is started",
									currentTime,
									vm.getId(),
									targetHost.getId());
						} else {
							Log.formatLine(
									"%.2f: Migration of VM #%d from Host #%d to Host #%d is started",
									currentTime,
									vm.getId(),
									oldHost.getId(),
									targetHost.getId());
						}

						targetHost.addMigratingInVm(vm);
						incrementMigrationCount();

						/** VM migration delay = RAM / bandwidth **/
						// we use BW / 2 to model BW available for migration purposes, the other
						// half of BW is for VM communication
						// around 16 seconds for 1024 MB using 1 Gbit/s network
						send(
								getId(),
								vm.getRam() / ((double) targetHost.getBw() / (2 * 8000)),
								CloudSimTags.VM_MIGRATE,
								migrate);
					}
				}
			}

			// schedules an event to the next time
			if (minTime != Double.MAX_VALUE) {//���minTime���double���͵ı���<Double�������Ƶĳ��ȡ�
				CloudSim.cancelAll(getId(), new PredicateType(CloudSimTags.VM_DATACENTER_EVENT));
				send(getId(), getSchedulingInterval(), CloudSimTags.VM_DATACENTER_EVENT);
			}

			setLastProcessTime(currentTime);
		}
	}

	/**
	 * Update cloudet processing without scheduling future events.
	 * 
	 * @return the double
	 */
	protected double updateCloudetProcessingWithoutSchedulingFutureEvents() {
		if (CloudSim.clock() > getLastProcessTime()) {
			return updateCloudetProcessingWithoutSchedulingFutureEventsForce();
		}
		return 0;
	}

	/**
	 * Update cloudet processing without scheduling future events.
	 * 
	 * @return the double
	 */
	protected double updateCloudetProcessingWithoutSchedulingFutureEventsForce() {
		double currentTime = CloudSim.clock();
		double minTime = Double.MAX_VALUE;
		double timeDiff = currentTime - getLastProcessTime();
		double timeFrameDatacenterEnergy = 0.0;

		Log.printLine("\n\n--------------------------------------------------------------\n\n");
		Log.formatLine("New resource usage for the time frame starting at %.2f:", currentTime);

		/*ѭ����������ÿ̨����������֪ͨVM����currentTime���´�������һ��ʱ��Ԥ������������Сʱ�䣬����ӡ��
		 * ��ǰ��ʱ�䣬��ǰ������ID��������CPU�����ʡ�
		 * 
		 * 
		 * ˵����
		 * 1.host.updateVmsProcessing(currentTime); 
		 * ����ֵ��cloudletԤ��Ҫ��ɵ�ʱ�䡣
		 * Time=smallerTime=nextEvent=estimateFinishTime =currentTime+rcl.getRemainingCloudletLenght()/(getCapacity(MispShare)*rcl.getNumberOfPes())
		 * updateCloudletProcessingWthoutSchedulingFutureEventsForce()
		 * -->Host.java:public double updateVmsProcessing(double currentTime):double time = vm.updateVmProcessing(currentTime, getVmScheduler().getAllocatedMipsForVm(vm));
		 * -->Vm.java:public double updateVmProcessing(double currentTime, List<Double> mipsShare) :return getCloudletScheduler().updateVmProcessing(currentTime, mipsShare);
		 * -->CloudletSchedulerTimeScheduler:double estimatedFinishTime = currentTime+ (rcl.getRemainingCloudletLength() / (getCapacity(mipsShare) * rcl.getNumberOfPes()));
		 * δ��������������
		 * */
		for (PowerHost host : this.<PowerHost> getHostList()) {
			Log.printLine();
			//���������ÿ̨������������������̣�����һ��Ԥ����ɵ�ʱ�䡣
			double time = host.updateVmsProcessing(currentTime); // inform VMs to update processing  ֪ͨ�����ȥ����������Ĵ�����̣�����Ԥ��Ҫ��ɵ�ʱ�䡣
			if (time < minTime) {//�Ƚϸ�����������Ԥ����ɵ�ʱ�䡣
				minTime = time;//�����������ص�Ԥ����ɵ����ٵ�ʱ����Ϊ�´��ڲ��¼���ʱ��ı�׼��
			}

			Log.formatLine(
					"%.2f: [Host #%d] utilization is %.2f%%",
					currentTime,
					host.getId(),
					host.getUtilizationOfCpu() * 100);
		}
		
		/*
		 * ���timeDiff > 0 ,ʱ���ӳ� >0
		 * {
		 * ��ӡ����  �ϴε�ʱ�䷶Χ��getLastProcessTime()��currentTime���ܺ�.
		 * 
		 * �������б����ȡ��������{
		 * 1.ȡ��   ��һ�Σ���ǰ����cpu�����ʡ���ǰ������cpu������ ��timeDiffһ����Ϊ�������ݽ�getEnergyLinearInterpolation(...)�����
		 * ���ϴε����ڵ�ʱ�䷶Χ���������ܺġ�(�������Բ�ֵ)
		 * 2.�����������İ�����������������ˣ��������ĵ��ܺľ��൱�����������ܺĵ��ܺͣ�timeFrameDatacenterEnergy += timeFrameHostEnergy;
		 * 3.��ӡ�� currentTime:[Host host.getId()] ����һ�δ���ʱ��Ϊ��xx%��������xx%��
		 * 	    ��ӡ��currentTime:[Host host.getId()]���� �� timeFrameHostEnergy W*sec
		 * }
		 * ��ӡ��currentTime:�������ĵ��ܺ�Ϊ��timeFrameDatacenterEnergy W*sec
		 * }
		 * ����Power��ֵ������getPower()+timeFramDatacenterEnergy��ֵ����this.power����װ�������ڱ����С�
		 * ÿ�θ���������Ĵ�����̶���һ�����۵Ĺ��̣�setPower(getPower() + timeFrameDatacenterEnergy);
		 * 
		 * ����������Ƿ���ɣ�
		 * 
		 * �Ƴ��Ѿ����������������
		 * 1.������ÿ̨������ÿ̨�����ٵ������Ѿ���������ÿ̨�����
		 * 2.���VM��Host�İ�
		 * 3.���Ѿ���������Vm��vmList���Ƴ���
		 * 
		 * ��ӡ�����ָ���
		 * 
		 * �����ε�currentTime��ΪLastProcessTime.
		 * 
		 * ����Ԥ��������������Сʱ�䡣
		 * */
		if (timeDiff > 0) {
			Log.formatLine(
					"\nEnergy consumption for the last time frame from %.2f to %.2f:",
					getLastProcessTime(),
					currentTime);

			for (PowerHost host : this.<PowerHost> getHostList()) {
				double previousUtilizationOfCpu = host.getPreviousUtilizationOfCpu();
				double utilizationOfCpu = host.getUtilizationOfCpu();
				double timeFrameHostEnergy = host.getEnergyLinearInterpolation(
						previousUtilizationOfCpu,
						utilizationOfCpu,
						timeDiff);
				timeFrameDatacenterEnergy += timeFrameHostEnergy;

				Log.printLine();
				Log.formatLine(
						"%.2f: [Host #%d] utilization at %.2f was %.2f%%, now is %.2f%%",
						currentTime,
						host.getId(),
						getLastProcessTime(),
						previousUtilizationOfCpu * 100,
						utilizationOfCpu * 100);
				Log.formatLine(
						"%.2f: [Host #%d] energy is %.2f W*sec",
						currentTime,
						host.getId(),
						timeFrameHostEnergy);
			}

			Log.formatLine(
					"\n%.2f: Data center's energy is %.2f W*sec\n",
					currentTime,
					timeFrameDatacenterEnergy);
		}

		setPower(getPower() + timeFrameDatacenterEnergy);

		checkCloudletCompletion();

		/** Remove completed VMs **/
		for (PowerHost host : this.<PowerHost> getHostList()) {
			for (Vm vm : host.getCompletedVms()) {
				getVmAllocationPolicy().deallocateHostForVm(vm);
				getVmList().remove(vm);
				Log.printLine("VM #" + vm.getId() + " has been deallocated from host #" + host.getId());
			}
		}

		Log.printLine();

		setLastProcessTime(currentTime);
		return minTime;
	}

	/*
	 * (non-Javadoc)
	 * @see org.cloudbus.cloudsim.Datacenter#processVmMigrate(org.cloudbus.cloudsim.core.SimEvent,
	 * boolean)
	 */
	@Override
	protected void processVmMigrate(SimEvent ev, boolean ack) {
		updateCloudetProcessingWithoutSchedulingFutureEvents();//��ȡ���˵ȴ����е�����£���������������Ĵ�����̡�
		super.processVmMigrate(ev, ack);//���ø���Datacenter���еķ�����
		SimEvent event = CloudSim.findFirstDeferred(getId(), new PredicateType(CloudSimTags.VM_MIGRATE));
		if (event == null || event.eventTime() > CloudSim.clock()) {//���deferred�����еĵ�һ���¼�eventΪ�ջ��߸��¼�Я����ʱ��>�����¼���
			updateCloudetProcessingWithoutSchedulingFutureEventsForce();// Update cloudet processing without scheduling future events.
		}
	}

	/*
	 * (non-Javadoc)
	 * @see cloudsim.Datacenter#processCloudletSubmit(cloudsim.core.SimEvent, boolean)
	 */
	@Override
	protected void processCloudletSubmit(SimEvent ev, boolean ack) {
		super.processCloudletSubmit(ev, ack);
		setCloudletSubmitted(CloudSim.clock());
	}

	/**
	 * Gets the power.
	 * 
	 * @return the power
	 */
	public double getPower() {
		return power;
	}

	/**
	 * Sets the power.
	 * 
	 * @param power the new power
	 */
	protected void setPower(double power) {
		this.power = power;
	}

	/**
	 * Checks if PowerDatacenter is in migration.
	 * 
	 * @return true, if PowerDatacenter is in migration
	 */
	protected boolean isInMigration() {
		boolean result = false;
		for (Vm vm : getVmList()) {
			if (vm.isInMigration()) {
				result = true;
				break;
			}
		}
		return result;
	}

	/**
	 * Checks if is disable migrations.
	 * 
	 * @return true, if is disable migrations
	 */
	public boolean isDisableMigrations() {
		return disableMigrations;
	}

	/**
	 * Sets the disable migrations.
	 * 
	 * @param disableMigrations the new disable migrations
	 */
	public void setDisableMigrations(boolean disableMigrations) {
		this.disableMigrations = disableMigrations;
	}

	/**
	 * Checks if is cloudlet submited.
	 * 
	 * @return true, if is cloudlet submited
	 */
	protected double getCloudletSubmitted() {
		return cloudletSubmitted;
	}

	/**
	 * Sets the cloudlet submited.
	 * 
	 * @param cloudletSubmitted the new cloudlet submited
	 */
	protected void setCloudletSubmitted(double cloudletSubmitted) {
		this.cloudletSubmitted = cloudletSubmitted;
	}

	/**
	 * Gets the migration count.
	 * 
	 * @return the migration count
	 */
	public int getMigrationCount() {
		return migrationCount;
	}

	/**
	 * Sets the migration count.
	 * 
	 * @param migrationCount the new migration count
	 */
	protected void setMigrationCount(int migrationCount) {
		this.migrationCount = migrationCount;
	}

	/**
	 * Increment migration count.
	 */
	protected void incrementMigrationCount() {
		setMigrationCount(getMigrationCount() + 1);
	}

}
