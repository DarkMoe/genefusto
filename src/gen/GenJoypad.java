package gen;

//	http://md.squee.co/315-5309
//	http://md.squee.co/Howto:Read_Control_Pads
public class GenJoypad {

	long control1 = 0xFF;
	long control2 = 0xFF;
	long control3 = 0xFF;
	
	int D, U, L, R, A, B, C, S;
	int D2, U2, L2, R2, A2, B2, C2, S2;
	
	boolean asserted1;
	boolean asserted2;
	
	void initialize() {
		D = 1;
		U = 1;
		L = 1;
		R = 1;
		A = 1;
		B = 1;
		C = 1;
		S = 1;
		
		D2 = 1;
		U2 = 1;
		L2 = 1;
		R2 = 1;
		A2 = 1;
		B2 = 1;
		C2 = 1;
		S2 = 1;
		
		writeDataRegister1(0x40);
		writeDataRegister2(0x40);
	}
	
	void writeDataRegister1(long data) {
		if ((data & 0x40) == 0) {
//			System.out.println("Deassert TH " + Long.toHexString(data));
			asserted1 = true;
		} else {
//			System.out.println("Assert TH " + Long.toHexString(data));
			asserted1 = false;
		}
	}
	
	int readDataRegister1() {
		int res;
		if (asserted1) {
			res = (S << 5) | (A << 4) | (D << 1) | (U);	//	 (00SA00DU)
		} else {
			res = 0xC0 | (C << 5) | (B << 4) | (R << 3) | (L << 2) | (D << 1) | (U);	//	 (11CBRLDU)
		}
		return res;
	}
	
	void writeDataRegister2(long data) {
		if ((data & 0x40) == 0) {
//			System.out.println("Assert TH " + Long.toHexString(data));
			asserted2 = true;
		} else {
//			System.out.println("Deassert TH " + Long.toHexString(data));
			asserted2 = false;
		}
	}
	
	int readDataRegister2() {
		if (asserted2) {
			return (S2 << 5) | (A2 << 4) | (D2 << 1) | (U2);	//	 (00SA00DU)
		} else {
			return 0xC0 | (C2 << 5) | (B2 << 4) | (R2 << 3) | (L2 << 2) | (D2 << 1) | (U2);	//	 (11CBRLDU)
		}
	}
	
	int readDataRegister3() {
		return 0x3F;
	}
	
	void writeControlRegister1(long data) {
//		System.out.println("control data port 1! " + Long.toHexString(data));
		control1 = data;
	}
	
	void writeControlRegister2(long data) {
//		System.out.println("control data port 2! " + Long.toHexString(data));
		control2 = data;
	}
	
	void writeControlRegister3(long data) {
//		System.out.println("control data port 3! " + Long.toHexString(data));
		control3 = data;
	}
	
	long readControlRegister1() {
		return control1;
	}
	
	long readControlRegister2() {
		return control2;
	}
	
	long readControlRegister3() {
		return control3;
	}
	
}
