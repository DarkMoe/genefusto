package gen.instruction;

import gen.Gen68;
import gen.GenInstruction;
import gen.Size;

public class CMPM implements GenInstructionHandler {

	final Gen68 cpu;
	
	public CMPM(Gen68 cpu) {
		this.cpu = cpu;
	}

//	NAME
//	CMPM -- Compare memory
//
//SYNOPSIS
//	CMPM	(Ay)+,(Ax)+
//
//	Size = (Byte, Word, Long)
//
//FUNCTION
//	Subtracts the source operand from the destination operand
//	and sets the condition codes according to the result. The destination
//	operand is NOT changed. Operands are always addressed with the
//	postincrement mode.
//
//FORMAT
//	-----------------------------------------------------------------
//	|15 |14 |13 |12 |11 |10 | 9 | 8 | 7 | 6 | 5 | 4 | 3 | 2 | 1 | 0 |
//	|---|---|---|---|-----------|---|-------|---|---|---|-----------|
//	| 1 | 0 | 1 | 1 |Ax REGISTER| 1 | SIZE  | 0 | 0 | 1 |Ay REGISTER|
//	----------------------------------------=========================
//	                                                  <ea>
//
//SIZE
//	00->one Byte operation
//	01->one Word operation
//	10->one Long operation
//
//REGISTER
//	Ax register specifies destination operand (for post-incrementation).
//	Ay register specifies source operand.
//
//RESULT
//	X - Not affected
//	N - Set if the result is negative. Cleared otherwise.
//	Z - Set if the result is zero. Cleared otherwise.
//	V - Set if an overflow occours. Cleared otherwise.
//	C - Set if a borrow occours. Cleared otherwise.
	
	@Override
	public void generate() {
		int base = 0xB108;
		GenInstruction ins = null;
		for (int size = 0; size < 3; size++) {
			if (size == 0b00) {
				ins = new GenInstruction() {
					@Override
					public void run(int opcode) {
						CMPMByte(opcode);
					}
				};
			} else if (size == 0b01) {
				ins = new GenInstruction() {
					@Override
					public void run(int opcode) {
						CMPMWord(opcode);
					}
				};
			} else if (size == 0b10) {
				ins = new GenInstruction() {
					@Override
					public void run(int opcode) {
						CMPMLong(opcode);
					}
				};
			}
			for (int axRegister = 0; axRegister < 8; axRegister++) {
				for (int ayRegister = 0; ayRegister < 8; ayRegister++) {
					int opcode = base | (axRegister << 9) | (size << 6) | ayRegister;
					cpu.addInstruction(opcode, ins);
				}
			}
		}
	}

	private void CMPMByte(int opcode) {
		int axRegister = (opcode >> 9) & 0x7;
		int ayRegister = (opcode & 0x7);
		
		Operation ax = cpu.resolveAddressingMode(Size.BYTE, 0b011, axRegister);	// force post increment mode
		long data = ax.getAddressingMode().getByte(ax);
		
		Operation ay = cpu.resolveAddressingMode(Size.BYTE, 0b011, ayRegister); // force post increment mode
		long toSub = ay.getAddressingMode().getByte(ay);
		
		long res = data - toSub;
		
		calcFlags(res, Size.BYTE.getMsb(), 0xFF);
	}
	
	private void CMPMWord(int opcode) {
		throw new RuntimeException("NO");
	}
	
	private void CMPMLong(int opcode) {
		int axRegister = (opcode >> 9) & 0x7;
		int ayRegister = (opcode & 0x7);
		
		Operation ax = cpu.resolveAddressingMode(Size.LONG, 0b011, axRegister);	// force post increment mode
		long data = ax.getAddressingMode().getLong(ax);
		
		Operation ay = cpu.resolveAddressingMode(Size.LONG, 0b011, ayRegister); // force post increment mode
		long toSub = ay.getAddressingMode().getLong(ay);
		
		long res = data - toSub;
		
		calcFlags(res, Size.LONG.getMsb(), 0xFFFF_FFFFL);
	}
	
	void calcFlags(long data, long msb, long maxSize) {	// TODO V
		if ((data & maxSize) == 0) {
			cpu.setZ();
		} else {
			cpu.clearZ();
		}
		if (((data & maxSize) & msb) > 0) {
			cpu.setN();
		} else {
			cpu.clearN();
		}
		if (data < 0) {	// validar esto
			cpu.setC();
		} else {
			cpu.clearC();
		}
	}
	
}
