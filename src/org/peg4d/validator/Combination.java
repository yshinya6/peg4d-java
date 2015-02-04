package org.peg4d.validator;

import java.util.List;

public class Combination {
	@SuppressWarnings("rawtypes")
	static public int[][] combinationList(List e, int r) {
		int[] array_E = new int[e.size()];
		for (int i = 0; i < e.size(); i++) {
			array_E[i] = (Integer) e.get(i);
		}

		return combinationList(array_E, r);
	}
	static public int[][] combinationList(int[] e, int r) {
		int[][] afterList = new int[combinationTotalNumber(e.length, r)][r];
		int[] beforeListIndex = new int[r];
		int afterListIndex = 0;

		for (int i = 0; i < beforeListIndex.length; i++) {
			beforeListIndex[i] = beforeListIndex.length - i - 1;
		}

		while (beforeListIndex[beforeListIndex.length - 1] <= e.length - r) {
			for (int i = 0; i < r; i++) {
				int work = e[beforeListIndex[i]];
				afterList[afterListIndex][i] = work;
}
			afterListIndex++;
			beforeListIndex[0]++;
			beforeListIndex = figureRise(beforeListIndex, e.length);
		}
		return afterList;
	}
	static public int combinationTotalNumber(int n, int r) {
		return permutationTotalNumber(n, r) / factorial(r);
	}
	static public int permutationTotalNumber(int n, int r) {
		int totalNumber = 1;

		for (int i = 0; i < r; i++)
			totalNumber *= (n - i);

		return totalNumber;
	}
	/* 階乗を返す */
	static public int factorial(int r) {
		return (r > 1) ? r * factorial(r - 1) : 1;
	}
	/* 桁上がり処理を行う。重複は自動スキップ */
	static public int[] figureRise(int[] list, int max) {
		for (int listIndex = 0; listIndex < list.length - 1 && list[listIndex] >= max - listIndex; listIndex++) {

			/* 下位桁を全てリセット */
			list[listIndex + 1]++;
			for (int listIndex_a = listIndex; listIndex_a >= 0; listIndex_a--) {
				list[listIndex_a] = list[listIndex_a + 1] + 1;
			}
		}
		return list;
	}
}