package gen.instruction;

import gen.Gen68;
import gen.GenInstruction;
import gen.Size;

public class ADDX implements GenInstructionHandler {

	final Gen68 cpu;
	
	public ADDX(Gen68 cpu) {
		this.cpu = cpu;
	}
	
//	NAME
//	ADDX -- Add integer with extend
//
//SYNOPSIS
//	ADDX	Dy,Dx
//	ADDX	-(Ay),-(Ax)
//
//	Size = (Byte, Word, Long)
//
//FUNCTION
//	Adds the source operand to the destination operand along with
//	the extend bit, and stores the result in the destination location.
//
//	1. Data register to data register: The operands are contained in the
//	   data registers specified in the instruction.
//
//	2. Memory to memory: The operands are addressed with the predecrement
//	   addressing mode using the address registers specified in the
//	   instruction.
//
//	The size of operation can be specified as byte, word, or long.
//
//	Normally the Z condition code bit is set via programming before the
//	start of an operation. That allows successful tests for zero results
//	upon completion of multiple-precision operations.
//
//FORMAT
//	-----------------------------------------------------------------
//	|15 |14 |13 |12 |11 |10 | 9 | 8 | 7 | 6 | 5 | 4 | 3 | 2 | 1 | 0 |
//	|---|---|---|---|-----------|---|-------|---|---|---|-----------|
//	| 1 | 1 | 0 | 1 |    Rx     | 1 | SIZE  | 0 | 0 |R/M|    Ry     |
//	-----------------------------------------------------------------
//
//	R/M = 0 -> data register
//	R/M = 1 -> address register
//	Rx:   destination register
//	Ry:   source register
//
//SIZE
//	00->one Byte operation
//	01->one Word operation
//	10->one Long operation
//
//RESULT
//	X - Set the same as the carry bit.
//	N - Set if the result is negative. Cleared otherwise.
//	Z - Cleared if the result is non-zero. Unchanged otherwise.
//	V - Set if an overflow is generated. Cleared otherwise.
//	C - Set if a carry is generated. Cleared otherwise.
	
	@Override
	public void generate() {
		generateDataOperation();
		generateAddressOperation();
	}
	
	private void generateDataOperation() {
		int base = 0xD100;
		GenInstruction ins = null;
		
		for (int size = 0; size < 3; size++) {
			if (size == 0b00) {
				ins = new GenInstruction() {
					@Override
					public void run(int opcode) {
						ADDXDataByte(opcode);
					}
				};
				
			} else if (size == 0b01) {
				ins = new GenInstruction() {
					@Override
					public void run(int opcode) {
						ADDXDataWord(opcode);
					}
				};
				
			} else if (size == 0b10) {
				ins = new GenInstruction() {
					@Override
					public void run(int opcode) {
						ADDXDataLong(opcode);
					}
				};
				
			}
			
			for (int rx = 0; rx < 8; rx++) {
				for (int ry = 0; ry < 8; ry++) {
					int opcode = base | (rx << 9) | (size << 6) | ry;
					cpu.addInstruction(opcode, ins);
				}
			}
		}
	}
	
	private void generateAddressOperation() {
		int base = 0xD108;
		GenInstruction ins = null;
		
		for (int size = 0; size < 3; size++) {
			if (size == 0b00) {
				ins = new GenInstruction() {
					@Override
					public void run(int opcode) {
						ADDXAddressByte(opcode);
					}
				};
				
			} else if (size == 0b01) {
				ins = new GenInstruction() {
					@Override
					public void run(int opcode) {
						ADDXAddressWord(opcode);
					}
				};
				
			} else if (size == 0b10) {
				ins = new GenInstruction() {
					@Override
					public void run(int opcode) {
						ADDXAddressLong(opcode);
					}
				};
				
			}
			
			for (int rx = 0; rx < 8; rx++) {
				for (int ry = 0; ry < 8; ry++) {
					int opcode = base | (rx << 9) | (size << 6) | ry;
					cpu.addInstruction(opcode, ins);
				}
			}
		}
	}


	private void ADDXDataByte(int opcode) {
		int rx = (opcode >> 9) & 0x7;
		int ry = (opcode & 0x7);
		
		long data = cpu.getD(ry) & 0xFF;
		long toAdd = cpu.getD(rx) & 0xFF;
		
		int extended = cpu.isX() ? 1 : 0;
		
		long tot = data + toAdd + extended;
		cpu.setDByte(rx, tot);
		
		calcFlags(tot, Size.BYTE.getMsb(), Size.BYTE.getMax());
	}
	
	private void ADDXDataWord(int opcode) {
		int rx = (opcode >> 9) & 0x7;
		int ry = (opcode & 0x7);
		
		long data = cpu.getD(ry) & 0xFFFF;
		long toAdd = cpu.getD(rx) & 0xFFFF;
		
		int extended = cpu.isX() ? 1 : 0;
		
		long tot = data + toAdd + extended;
		cpu.setDWord(ry, tot);
		
		calcFlags(tot, Size.WORD.getMsb(), Size.WORD.getMax());
	}
	
	private void ADDXDataLong(int opcode) {
		int rx = (opcode >> 9) & 0x7;
		int ry = (opcode & 0x7);
		
		long data = cpu.getD(ry);
		long toAdd = cpu.getD(rx);
		
		int extended = cpu.isX() ? 1 : 0;
		
		long tot = data + toAdd + extended;
		cpu.setDLong(ry, tot);
		
		calcFlags(tot, Size.LONG.getMsb(), Size.LONG.getMax());
	}
	
	private void ADDXAddressByte(int opcode) {
		int rx = (opcode >> 9) & 0x7;
		int ry = (opcode & 0x7);
		
		long tot = 0;
		
		calcFlags(tot, Size.BYTE.getMsb(), 0xFF);
		throw new RuntimeException("NOT IM");
	}
	
	private void ADDXAddressWord(int opcode) {
		int rx = (opcode >> 9) & 0x7;
		int ry = (opcode & 0x7);
		
		long tot = 0;
		
		calcFlags(tot, Size.WORD.getMsb(), 0xFFFF);
		throw new RuntimeException("NOT IM");
	}
	
	private void ADDXAddressLong(int opcode) {
		int rx = (opcode >> 9) & 0x7;
		int ry = (opcode & 0x7);
		
		long tot = 0;
		
		calcFlags(tot, Size.LONG.getMsb(), 0xFFFF_FFFFL);
		throw new RuntimeException("NOT IM");
	}
	
	void calcFlags(long tot, long msb, long maxSize) {	//TODO  overflow
		if ((tot & maxSize) == 0) {
			cpu.setZ();
		} else {
			cpu.clearZ();
		}
		if ((tot & msb) > 0) {
			cpu.setN();
		} else {
			cpu.clearN();
		}
		if (tot > maxSize) {
			cpu.setC();
			cpu.setX();
		} else {
			cpu.clearC();
			cpu.clearX();
		}
	}
	
}
