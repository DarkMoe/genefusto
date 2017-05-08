package gen;

//	http://md.squee.co/315-5309
//	http://md.squee.co/Howto:Read_Control_Pads
public class GenJoypad {

	long control1;
	long control2;
	long control3;
	
	boolean asserted;
	
	void writeDataRegister1(long data) {
		if (data == 0) {
			System.out.println("Assert TH");
			asserted = true;
		} else {
			System.out.println("Deassert TH " + data);
			asserted = false;
		}
	}
	
	int readDataRegister1() {
		if (asserted) {
			return 0x33;
		} else {
//			return 0x7F;
			return 0x3F;	// simpsons devuelve 3F al ppio
		}
	}
	
	void writeDataRegister2(long data) {
		System.out.println("data reg 2! " + Long.toHexString(data));
	}
	
	int readDataRegister2() {
		return 0;
	}
	
	void writeControlRegister1(long data) {
		System.out.println("control data! " + Long.toHexString(data));
		control1 = data;
	}
	
	void writeControlRegister2(long data) {
		System.out.println("control data! " + Long.toHexString(data));
	}
	
	void writeControlRegister3(long data) {
		System.out.println("control data! " + Long.toHexString(data));
	}
	
}
