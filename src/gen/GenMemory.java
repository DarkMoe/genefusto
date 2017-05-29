package gen;

public class GenMemory {

	int[] cartridge;
	int[] ram = new int[0x10000];
	
	long readCartridgeByte(long address) {
		long data = 0;
		if (address <= 0x3FFFFF) {
			if (address >= cartridge.length) {	//	wrapping ? TODO confirmar
				address -= cartridge.length;
			}
			data = cartridge[(int) address];
		}
		return data;
	}
	
	long readCartridgeWord(long address) {
		long data = 0;
		if (address <= 0x3FFFFF) {
			if (address >= cartridge.length) {	//	wrapping ? TODO confirmar
				address -= cartridge.length;
			}
			data  = cartridge[(int) address] << 8;
			data |= cartridge[(int) address + 1];
		}
		return data;
	}
	
	long readRam(long address) {
		long data = 0;
		if (address >= 0xFF0000) {
			data = ram[(int) (address - 0xFF0000)];
		}
		return data;
	}
	
	void writeRam(long address, long data) {
		if (address == 0xc2bb){
			System.out.println();
		}
		if (address <= 0xFFFF) {
			ram[(int) address] = (int) data;
		} else {
			throw new RuntimeException("READ NOT MAPPED: " + Integer.toHexString((int) address));
		}
	}
	
}
