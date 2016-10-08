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
 * PowerDatacenter 是一个启用能耗监控仿真数据中心的类。
 * 
 * If you are using any algorithms, policies or workload included in the power package please cite
 * the following paper:
 * 如果你将使用任意的算法、策略或者包含在Power包下工作负载，请引用以下的论文：
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
	 * Instantiates a new datacenter. 实例化一个新的数据中心
	 * 
	 * @param name the name
	 * @param characteristics the res config
	 * @param schedulingInterval the scheduling interval（间隔）
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
			 * 如果是无效的迁移的话.
			 * 做如下的操作：
			 * 从虚拟机的最优迁移分配策略中取得一个存储着Map<String, Object>类型的数据的列表。
			 * 
			 * 判定该列表不为空的情况下，迭代出每个map集合,从集合中分别取出VM和Host。
			 * 从Vm中取出现在自己所绑定的主机host.
			 * 
			 * targetHost：map集合中取出的理想的主机Host。
			 * oldHost：vm绑定的主机Host。
			 * 
			 * 若Vm没有绑定任何主机，直接将该虚拟机绑定到targetHost理想的目标主机。
			 * 若Vm有已经绑定了一台主机，则将该vm从oldHost迁移到targetHost上。targetHost.addMigratingInVm(vm);
			 * 
			 * 统计迁移的虚拟机数目。
			 * 未深入剖析：List<Map<String, Object>> migrationMap = getVmAllocationPolicy().optimizeAllocation(getVmList());
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
			if (minTime != Double.MAX_VALUE) {//如果minTime这个double类型的变量<Double类型限制的长度。
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

		/*循环遍历处理每台主机，主机通知VM基于currentTime更新处理，返回一个时间预计完成任务的最小时间，并打印：
		 * 当前的时间，当前主机的ID，主机的CPU利用率。
		 * 
		 * 
		 * 说明：
		 * 1.host.updateVmsProcessing(currentTime); 
		 * 返回值：cloudlet预计要完成的时间。
		 * Time=smallerTime=nextEvent=estimateFinishTime =currentTime+rcl.getRemainingCloudletLenght()/(getCapacity(MispShare)*rcl.getNumberOfPes())
		 * updateCloudletProcessingWthoutSchedulingFutureEventsForce()
		 * -->Host.java:public double updateVmsProcessing(double currentTime):double time = vm.updateVmProcessing(currentTime, getVmScheduler().getAllocatedMipsForVm(vm));
		 * -->Vm.java:public double updateVmProcessing(double currentTime, List<Double> mipsShare) :return getCloudletScheduler().updateVmProcessing(currentTime, mipsShare);
		 * -->CloudletSchedulerTimeScheduler:double estimatedFinishTime = currentTime+ (rcl.getRemainingCloudletLength() / (getCapacity(mipsShare) * rcl.getNumberOfPes()));
		 * 未深入剖析备忘：
		 * */
		for (PowerHost host : this.<PowerHost> getHostList()) {
			Log.printLine();
			//这里迭代出每台主机并更新云任务进程，返回一个预计完成的时间。
			double time = host.updateVmsProcessing(currentTime); // inform VMs to update processing  通知虚拟机去更新云任务的处理进程，返回预计要完成的时间。
			if (time < minTime) {//比较各次主机返回预计完成的时间。
				minTime = time;//各次主机返回的预计完成的最少的时间作为下次内部事件的时间的标准。
			}

			Log.formatLine(
					"%.2f: [Host #%d] utilization is %.2f%%",
					currentTime,
					host.getId(),
					host.getUtilizationOfCpu() * 100);
		}
		
		/*
		 * 如果timeDiff > 0 ,时间延迟 >0
		 * {
		 * 打印出：  上次的时间范围从getLastProcessTime()到currentTime的能耗.
		 * 
		 * 从主机列表遍历取出主机：{
		 * 1.取得   上一次（先前）的cpu利用率、当前主机的cpu利用率 与timeDiff一起作为参数传递进getEnergyLinearInterpolation(...)计算出
		 * 自上次到现在的时间范围内主机的能耗。(基于线性插值)
		 * 2.由于数据中心包含大量的主机，因此，数据中心的能耗就相当于所有主机能耗的总和：timeFrameDatacenterEnergy += timeFrameHostEnergy;
		 * 3.打印： currentTime:[Host host.getId()] 在上一次处理时刻为：xx%，现在是xx%。
		 * 	    打印：currentTime:[Host host.getId()]能量 是 timeFrameHostEnergy W*sec
		 * }
		 * 打印：currentTime:数据中心的能耗为：timeFrameDatacenterEnergy W*sec
		 * }
		 * 设置Power的值。即将getPower()+timeFramDatacenterEnergy的值赋给this.power。封装好数据在本类中。
		 * 每次更新云任务的处理进程都是一个积累的过程：setPower(getPower() + timeFrameDatacenterEnergy);
		 * 
		 * 检查云任务是否完成：
		 * 
		 * 移除已经完成任务的虚拟机。
		 * 1.迭代出每台主机，每台主机再迭代出已经完成任务的每台虚拟机
		 * 2.解除VM与Host的绑定
		 * 3.将已经完成任务的Vm从vmList中移除。
		 * 
		 * 打印出：分隔符
		 * 
		 * 将本次的currentTime作为LastProcessTime.
		 * 
		 * 返回预计完成云任务的最小时间。
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
		updateCloudetProcessingWithoutSchedulingFutureEvents();//在取消了等待队列的情况下，继续更新云任务的处理进程。
		super.processVmMigrate(ev, ack);//调用父类Datacenter的中的方法，
		SimEvent event = CloudSim.findFirstDeferred(getId(), new PredicateType(CloudSimTags.VM_MIGRATE));
		if (event == null || event.eventTime() > CloudSim.clock()) {//如果deferred队列中的第一个事件event为空或者该事件携带的时间>仿真事件。
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
