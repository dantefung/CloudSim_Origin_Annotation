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
		setSchedulingInterval(schedulingInterval);//���õ��ȼ��
		BufferedReader input = new BufferedReader(new FileReader(inputPath));//������ָ���ļ������롣���û�л��壬��ÿ�ε��� read() �� readLine() ���ᵼ�´��ļ��ж�ȡ�ֽڣ�������ת��Ϊ�ַ��󷵻أ������Ǽ����Ч�ġ� 
		int n = data.length;//	private final double[] data = new double[289];
		for (int i = 0; i < n - 1; i++) {
			data[i] = Integer.valueOf(input.readLine()) / 100.0;//��ȡһ���ı���/100.0  ���磺��workload/planetlab/888/surfsuel_dsl_internl_net_colostate_577�µĵ�һ�У�24 ��data[1]=0.24
			System.out.println(data[i]);//new add by dantefung
		}
		data[n - 1] = data[n - 2];//data[288] = data[287]
		input.close();// �رո������ͷ���֮������������Դ��
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
