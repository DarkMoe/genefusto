package gen.instruction;

import gen.Gen68;
import gen.GenInstruction;

public class EXG implements GenInstructionHandler {

	final Gen68 cpu;
	
	public EXG(Gen68 cpu) {
		this.cpu = cpu;
	}
	
//	NAME
//	EXG -- Register exchange
//
//SYNOPSIS
//	EXG	Rx,Ry
//
//	Size = (Long)
//
//FUNCTION
//	Exchanges the contents of any two registers.
//
//FORMAT
//	-----------------------------------------------------------------
//	|15 |14 |13 |12 |11 |10 | 9 | 8 | 7 | 6 | 5 | 4 | 3 | 2 | 1 | 0 |
//	|---|---|---|---|-----------|---|-------------------|-----------|
//	| 1 | 1 | 0 | 0 |Rx REGISTER| 1 |       OP-MODE     |Ry REGISTER|
//	-----------------------------------------------------------------
//
//	"Rx REGISTER" specifies a data or address register. If it's an
//	exchange between a data register and an address register, this field
//	define the data register.
//
//	"Ry REGISTER" specifies a data or address register. If it's an
//	exchange between a data register and an address register, this field
//	define the address register.
//
//OP-MODE
//	01000->Exchange between data registers.
//	01001->Exchange between address registers.
//	10001->Exchange between data and address registers.
//
//RESULT
//	None.
	
	@Override
	public void generate() {
		int base = 0xC100;
		GenInstruction ins = null;
		
		int opMode = 0;
		for (int i = 0; i < 3; i++) {
			if (i == 0) {
				opMode = 0b01000;
				ins = new GenInstruction() {
					
					@Override
					public void run(int opcode) {
						EXGDataRegs(opcode);
					}
					
				};
			} else if (i == 1) {
				opMode = 0b01001;
				ins = new GenInstruction() {
					
					@Override
					public void run(int opcode) {
						EXGAddressRegs(opcode);
					}
					
				};
			} else if (i == 2) {
				opMode = 0b10001;
				ins = new GenInstruction() {
					
					@Override
					public void run(int opcode) {
						EXGDataAndAddressRegs(opcode);
					}
					
				};
			}
			for (int rxRegister = 0; rxRegister < 8; rxRegister++) {
				for (int ryRegister = 0; ryRegister < 8; ryRegister++) {
					int opcode = base | (rxRegister << 9) | (opMode << 3) | ryRegister;
					cpu.addInstruction(opcode, ins);
				}
			}
		}
	}
	
	private void EXGDataRegs(int opcode) {
		int rxRegister = (opcode >> 9) & 0x7;
		int ryRegister = opcode & 0x7;
		
		long rx = cpu.getDLong(rxRegister);
		long ry = cpu.getDLong(ryRegister);

		cpu.setDLong(rxRegister, ry);
		cpu.setDLong(ryRegister, rx);
	}
	
	private void EXGAddressRegs(int opcode) {
		int rxRegister = (opcode >> 9) & 0x7;
		int ryRegister = opcode & 0x7;
		
		long rx = cpu.getALong(rxRegister);
		long ry = cpu.getALong(ryRegister);

		cpu.setALong(rxRegister, ry);
		cpu.setALong(ryRegister, rx);
	}
	
	private void EXGDataAndAddressRegs(int opcode) {
		int rxRegister = (opcode >> 9) & 0x7;
		int ryRegister = opcode & 0x7;
		
		long rx = cpu.getDLong(rxRegister);
		long ry = cpu.getALong(ryRegister);

		cpu.setDLong(rxRegister, ry);
		cpu.setALong(ryRegister, rx);
	}
	
}
