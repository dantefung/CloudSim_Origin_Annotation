package org.cloudbus.cloudsim.examples.power.planetlab;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;

import org.cloudbus.cloudsim.Cloudlet;
import org.cloudbus.cloudsim.UtilizationModel;
import org.cloudbus.cloudsim.UtilizationModelNull;
import org.cloudbus.cloudsim.UtilizationModelPlanetLabInMemory;
import org.cloudbus.cloudsim.examples.power.Constants;

/**
 * A helper class for the running examples for the PlanetLab workload.
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
 * @since Jan 5, 2012
 */
public class PlanetLabHelper {

	/**
	 * Creates the cloudlet list planet lab.
	 * 
	 * @param brokerId the broker id
	 * @param inputFolderName the input folder name
	 * @return the list
	 * @throws FileNotFoundException the file not found exception
	 */
	public static List<Cloudlet> createCloudletListPlanetLab(int brokerId, String inputFolderName)
			throws FileNotFoundException {
		List<Cloudlet> list = new ArrayList<Cloudlet>();//ʵ�ִ�����һ���б������洢������

		long fileSize = 300;//�ļ���СΪ300
		long outputSize = 300;//�����СΪ300
		UtilizationModel utilizationModelNull = new UtilizationModelNull();//The UtilizationModelNull class is a simple model, according to which a Cloudlet always require zero capacity.

		File inputFolder = new File(inputFolderName);//����File����󣬴�������·��:workload/planetlab/888��
		File[] files = inputFolder.listFiles();//����һ�� File �������飬ÿ������Ԫ�س���·������ӦĿ¼�е�ÿ���ļ���Ŀ¼����Ȼ����ʱworkload/planetlab/888Ŀ¼��ֻ��һ���ļ���

		for (int i = 0; i < files.length; i++) {//����workload/panetlab/888Ŀ¼�µ��ļ���������������.
			Cloudlet cloudlet = null;
			try {
				cloudlet = new Cloudlet(
						i,//�������ID�š�
						Constants.CLOUDLET_LENGTH,//���񳤶�
						Constants.CLOUDLET_PES,//������Ҫ��cpu����Ԫ������
						fileSize,//�ļ���СΪ300.
						outputSize,//�����СΪ300.
						new UtilizationModelPlanetLabInMemory(
								files[i].getAbsolutePath(),//�ļ��ľ���·����
								Constants.SCHEDULING_INTERVAL/*���ȼ��*/), utilizationModelNull, utilizationModelNull);
			} catch (Exception e) {
				e.printStackTrace();
				System.exit(0);
			}
			cloudlet.setUserId(brokerId);//���������������Ĵ�������ϵ��
			cloudlet.setVmId(i);//�������������������һ��
			list.add(cloudlet);
		}

		return list;
	}

}
