package gen.instruction;

import gen.Gen68;
import gen.GenInstruction;
import gen.Size;

public class SBCD implements GenInstructionHandler {

	final Gen68 cpu;
	
	public SBCD(Gen68 cpu) {
		this.cpu = cpu;
	}

//	NAME
//	SBCD -- Subtract binary coded decimal with extend
//
//SYNOPSIS
//	SBCD	Dy,Dx
//	SBCD	-(Ay),-(Ax)
//
//	Size = (Byte)
//
//FUNCTION
//	Subtracts the source operand to the destination operand along with
//	the extend bit, and stores the result in the destination location.
//        The subtraction is performed using binary coded decimal arithmetic.
//        The operands, which are packed BCD numbers, can be addressed in two
//        different ways:
//
//	1. Data register to data register: The operands are contained in the
//	   data registers specified in the instruction.
//
//	2. Memory to memory: The operands are addressed with the predecrement
//	   addressing mode using the address registers specified in the
//	   instruction.
//
//	Normally the Z condition code bit is set via programming before the
//	start of an operation. That allows successful tests for zero results
//	upon completion of multiple-precision operations.
//
//FORMAT
//	-----------------------------------------------------------------
//	|15 |14 |13 |12 |11 |10 | 9 | 8 | 7 | 6 | 5 | 4 | 3 | 2 | 1 | 0 |
//	|---|---|---|---|-----------|---|---|---|---|---|---|-----------|
//	| 1 | 0 | 0 | 0 |Ry REGISTER| 1 | 0 | 0 | 0 | 0 |R/M|Rx REGISTER|
//	-----------------------------------------------------------------
//
//	Ry specifies destination register.
//	Rx specifies source register.
//	If R/M = 0: Rx and Ry are datas registers.
//	If R/M = 1: Rx and Ry are address registers used  for the pre-
//	decrementing.
//
//RESULT
//	X - Set the same as the carry bit.
//	N - Undefined
//	Z - Cleared if the result is non-zero. Unchanged otherwise.
//	V - Undefined
//	C - Set if a decimal carry was generated. Cleared otherwise.

	@Override
	public void generate() {
		generateDataOperation();
		generateAddressOperation();
	}
	
	private void generateDataOperation() {
		int base = 0x8100;
		GenInstruction ins = null;
		
		ins = new GenInstruction() {
			@Override
			public void run(int opcode) {
				SBCDDataByte(opcode);
			}
		};
				
		for (int rx = 0; rx < 8; rx++) {
			for (int ry = 0; ry < 8; ry++) {
				int opcode = base | (rx << 9) | ry;
				cpu.addInstruction(opcode, ins);
			}
		}
	}
	
	private void generateAddressOperation() {
		int base = 0x8108;
		GenInstruction ins = null;

		ins = new GenInstruction() {
			@Override
			public void run(int opcode) {
				SBCDAddressByte(opcode);
			}
		};
			
		for (int rx = 0; rx < 8; rx++) {
			for (int ry = 0; ry < 8; ry++) {
				int opcode = base | (rx << 9) | ry;
				cpu.addInstruction(opcode, ins);
			}
		}
	}

	private void SBCDDataByte(int opcode) {
		int rx = (opcode >> 9) & 0x7;
		int ry = (opcode & 0x7);
		
		long data = cpu.getDByte(ry);
		long toAdd = cpu.getDByte(rx);
		
		long tot = doCalc(data, toAdd);
		cpu.setDByte(rx, tot);
	}
	
	private void SBCDAddressByte(int opcode) {
		int rx = (opcode >> 9) & 0x7;
		int ry = (opcode & 0x7);
		
		long source = cpu.getALong(ry);
		long dest = cpu.getALong(rx);
		
		source--;
		dest--;
		
		cpu.setALong(ry, source);
		cpu.setALong(rx, dest);
		
		Operation o = cpu.resolveAddressingMode(Size.BYTE, 0b010, ry);	//	address indirect
		long data = o.getAddressingMode().getByte(o);
		
		Operation o2 = cpu.resolveAddressingMode(Size.BYTE, 0b010, rx);	//	address indirect
		long toAdd = o2.getAddressingMode().getByte(o2);
		
		long tot = doCalc(data, toAdd);
		cpu.writeKnownAddressingMode(o2, tot, Size.BYTE);
	}
	
	protected final long doCalc(long s, long d) {
		int x = (cpu.isX() ? 1 : 0);
		int c;

		long lo = (d & 0x0F) - (s & 0x0F) - x;
		if (lo < 0) {
			lo += 10;
			c = 1;
		} else {
			c = 0;
		}

		long hi = ((d >> 4) & 0x0F) - ((s >> 4) & 0x0F) - c;
		if (hi < 0) {
			hi += 10;
			c = 1;
		} else {
			c = 0;
		}

		long result = (hi << 4) + lo;

		if (c != 0) {
			cpu.setC();
			cpu.setX();
		} else {
			cpu.clearC();
			cpu.clearX();
		}

		if (result != 0) {
			cpu.clearZ();
		}

		return result;
	}
	
}
