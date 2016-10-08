package org.cloudbus.cloudsim;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

/**
 * The Class UtilizationModelPlanetLab.
 */
public class UtilizationModelPlanetLabInMemory implements UtilizationModel {

	/** The scheduling interval. */
	private double schedulingInterval;

	/** The data (5 min * 288 = 24 hours). */
	private final double[] data = new double[289];

	/**
	 * Instantiates a new utilization model PlanetLab.
	 * 
	 * @param inputPath the input path
	 * @throws NumberFormatException the number format exception
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
 	public UtilizationModelPlanetLabInMemory(String inputPath/*workload/planetlab/888/surfsuel_dsl_internl_net_colostate_577*/, double schedulingInterval)
			throws NumberFormatException,
			IOException {
		setSchedulingInterval(schedulingInterval);//设置调度间隔
		BufferedReader input = new BufferedReader(new FileReader(inputPath));//将缓冲指定文件的输入。如果没有缓冲，则每次调用 read() 或 readLine() 都会导致从文件中读取字节，并将其转换为字符后返回，而这是极其低效的。 
		int n = data.length;//	private final double[] data = new double[289];
		for (int i = 0; i < n - 1; i++) {
			data[i] = Integer.valueOf(input.readLine()) / 100.0;//读取一个文本行/100.0  例如：在workload/planetlab/888/surfsuel_dsl_internl_net_colostate_577下的第一行：24 则data[1]=0.24
			System.out.println(data[i]);//new add by dantefung
		}
		data[n - 1] = data[n - 2];//data[288] = data[287]
		input.close();// 关闭该流并释放与之关联的所有资源。
	}

	/*
	 * (non-Javadoc)
	 * @see cloudsim.power.UtilizationModel#getUtilization(double)
	 */
	@Override
	public double getUtilization(double time) {
		if (time % getSchedulingInterval() == 0) {
			return data[(int) time / (int) getSchedulingInterval()];
		}
		int time1 = (int) Math.floor(time / getSchedulingInterval());
		int time2 = (int) Math.ceil(time / getSchedulingInterval());
		double utilization1 = data[time1];
		double utilization2 = data[time2];
		double delta = (utilization2 - utilization1) / ((time2 - time1) * getSchedulingInterval());
		double utilization = utilization1 + delta * (time - time1 * getSchedulingInterval());
		return utilization;

	}

	/**
	 * Sets the scheduling interval.
	 * 
	 * @param schedulingInterval the new scheduling interval
	 */
	public void setSchedulingInterval(double schedulingInterval) {
		this.schedulingInterval = schedulingInterval;
	}

	/**
	 * Gets the scheduling interval.
	 * 
	 * @return the scheduling interval
	 */
	public double getSchedulingInterval() {
		return schedulingInterval;
	}
}
