package gen.instruction;

import gen.Gen68;
import gen.GenInstruction;
import gen.Size;

public class PEA implements GenInstructionHandler {

	final Gen68 cpu;
	
	public PEA(Gen68 cpu) {
		this.cpu = cpu;
	}

//	NAME
//	PEA -- Push effective address
//
//SYNOPSIS
//	PEA	<ea>
//
//	Size = (Long)
//
//FUNCTION
//	Effective address is stored to stack. Stack is decreased.
//
//FORMAT
//	-----------------------------------------------------------------
//	|15 |14 |13 |12 |11 |10 | 9 | 8 | 7 | 6 | 5 | 4 | 3 | 2 | 1 | 0 |
//	|---|---|---|---|---|---|---|---|---|---|-----------|-----------|
//	| 0 | 1 | 0 | 0 | 1 | 0 | 0 | 0 | 0 | 1 |    MODE   | REGISTER  |
//	----------------------------------------=========================
//	                                                   <ea>
//
//REGISTER
//	<ea> specifies destination operand, allowed addressing modes are:
//	--------------------------------- -------------------------------
//	|Addressing Mode|Mode| Register | |Addressing Mode|Mode|Register|
//	|-------------------------------| |-----------------------------|
//	|      Dn       | -  |    -     | |    Abs.W      |111 |  000   |
//	|-------------------------------| |-----------------------------|
//	|      An       | -  |    -     | |    Abs.L      |111 |  001   |
//	|-------------------------------| |-----------------------------|
//	|     (An)      |010 |N° reg. An| |   (d16,PC)    |111 |  010   |
//	|-------------------------------| |-----------------------------|
//	|     (An)+     | -  |    -     | |   (d8,PC,Xi)  |111 |  011   |
//	|-------------------------------| |-----------------------------|
//	|    -(An)      | -  |    -     | |   (bd,PC,Xi)  |111 |  011   |
//	|-------------------------------| |-----------------------------|
//	|    (d16,An)   |101 |N° reg. An| |([bd,PC,Xi],od)|111 |  011   |
//	|-------------------------------| |-----------------------------|
//	|   (d8,An,Xi)  |110 |N° reg. An| |([bd,PC],Xi,od)|111 |  011   |
//	|-------------------------------| |-----------------------------|
//	|   (bd,An,Xi)  |110 |N° reg. An| |    #data      | -  |   -    |
//	|-------------------------------| -------------------------------
//	|([bd,An,Xi]od) |110 |N° reg. An|
//	|-------------------------------|
//	|([bd,An],Xi,od)|110 |N° reg. An|
//	---------------------------------
//
//RESULT
//	None.
	
	@Override
	public void generate() {
		int base = 0x4840;
		GenInstruction ins;
		
		ins = new GenInstruction() {
			
			@Override
			public void run(int opcode) {
				PEALong(opcode);
			}
			
		};
		
		for (int m = 0; m < 8; m++) {
			for (int r = 0; r < 8; r++) {
				if (m == 0b000 || m == 0b001 || m == 0b011 || m == 0b100) {
					continue;
				}
				if (m == 0b111 && r > 0b011) {
					continue;
				}
				
				int opcode = base | (m << 3) | r;
				cpu.addInstruction(opcode, ins);
			}
		}
		
	}
	
	private void PEALong(int opcode) {
		int mode = (opcode >> 3) & 0x7;
		int register = (opcode & 0x7);
		
		Operation o = cpu.resolveAddressingMode(Size.LONG, mode, register);
		long addr = o.getAddress();
		
		if ((cpu.SR & 0x2000) == 0x2000) {
			cpu.SSP--;
			cpu.bus.write(cpu.SSP, addr & 0xFF, Size.BYTE);
			cpu.SSP--;
			cpu.bus.write(cpu.SSP, (addr >> 8) & 0xFF, Size.BYTE);
			cpu.SSP--;
			cpu.bus.write(cpu.SSP, (addr >> 16) & 0xFF, Size.BYTE);
			cpu.SSP--;
			cpu.bus.write(cpu.SSP, (addr >> 24), Size.BYTE);
			
			cpu.setALong(7, cpu.SSP);
		} else {
			cpu.USP--;
			cpu.bus.write(cpu.USP, addr & 0xFF, Size.BYTE);
			cpu.USP--;
			cpu.bus.write(cpu.USP, (addr >> 8) & 0xFF, Size.BYTE);
			cpu.USP--;
			cpu.bus.write(cpu.USP, (addr >> 16) & 0xFF, Size.BYTE);
			cpu.USP--;
			cpu.bus.write(cpu.USP, (addr >> 24), Size.BYTE);
			
			cpu.setALong(7, cpu.USP);
		}
		
	}

}
