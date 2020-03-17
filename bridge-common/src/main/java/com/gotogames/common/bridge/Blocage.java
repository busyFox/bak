package com.gotogames.common.bridge;

public enum Blocage {
	_Niveau_0(0),_Niveau_1_A(1),_Niveau_1_B(2),_Niveau_2_A(3),_Niveau_2_B(4),_Niveau_3_A(5),_Niveau_3_B(6),_Niveau_4_A(7),_Niveau_4_B(8),_Niveau_5_A(9),_Niveau_5_B(10),_Niveau_6(11),_Niveau_Inconnu(12);
	private int n;
	
	Blocage(int i)
	{
		n = i ;
	}
	
	int getInt()
	{
		return n;
	}
}
