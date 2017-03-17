package gen;

public class Gen68 {
	
	//	D0-D7
	public long[] D = new long[8];
	//	A0-A7	(A7 = USP = User Stack Pointer ?)
	public long[] A = new long[8];
	
	public long PC;

	//	Supervisor SP
	public int SSP;
	//	User SP
	public long USP;
	
	//	http://tict.ticalc.org/docs/68kguide.txt
	//	Status Register o condition code register
	//	X—Extend: Set to the value of the C-bit for arithmetic operations; otherwise not affected or set to a specified result.
	//	N—Negative: Set if the most significant bit of the result is set; otherwise clear.
	//	Z—Zero: Set if the result equals zero; otherwise clear.
	//	V—Overflow: Set if an arithmetic overflow occurs implying that the result cannot be represented in the operand size; otherwise clear. 
	//	C—Carry: Set if a carry out of the most significant bit of the operand occurs for an addition, or if a borrow occurs in a subtraction; otherwise clear. 
	
//	 Bit 15 14 13 12 11 10  9  8  7  6  5  4  3  2  1  0
//	    -------------------------------------------------
//	    | T| -| S| -| -| I2,1,0 | -| -| -| X| N| Z| V| C|   - the status register
//	    -------------------------------------------------

//Bit 15: T - The trace bit. If it's set an interrupts will be called after
//        each instruction. Often used in debuggers.
//
//Bit 13: S - Supervisor bit. When this bit is set, you have more "access" to
//        some instructions and also to the systembyte. The reason for this is
//        that it prevents programs to disturb the OS with some instructions
//        that you shouldn't use if you're not writing an OS. Is enabled
//        when interruptions are generated.
//
//Bit 8-10: The interrupt mask
//
// The I0, I1 and I2 bits of the system register are used to set the interrupt
//mask: in fact, it means that they are set to an interupt level: if the trap 
//generated has a level higher than the interrupt mask, then the trap is
//executed. Otherwise it is ignored. ( ignoring a trap generally means that
//another interrupt, with a higher priority is beeing treated )
//
//Here is how these bits are set:
//
//        | I2 | I1 | I0 |
//
//level 0 |  0    0    0 |  ---------> lowest priority
//level 1 |  0    0    1 |
//level 2 |  0    1    0 |
//level 3 |  0    1    1 |
//level 4 |  1    0    0 |
//level 5 |  1    0    1 |
//level 6 |  1    1    0 |
//level 7 |  1    1    1 |  ---------> highest priority
	public int SR;
	
	//	8 Floating-Point Data Registers (FP7 – FP0) 
	float[] FP = new float[8];
	//	16-Bit Floating-Point Control Register (FPCR) 
	float FPCR;
	//	32-Bit Floating-Point Status Register (FPSR) 
	float FPSR;
	//	32-Bit Floating-Point Instruction Address Register (FPIAR) 
	float FPIAR;
	
	public GenBus bus;
	
	Gen68(GenBus bus) {
		this.bus = bus;
	}
	
	int cycles = 0;
	
	GenInstruction[] instructions = new GenInstruction[0x10000];
	
	public int runInstruction() {
		
		long opcode = (bus.read(PC) << 8);
		opcode |= bus.read(PC + 1);
		
		System.out.println(pad4((int) PC) + " - Opcode: " + pad4((int) opcode) + " - SR: " + pad4(SR));
		
		if (PC == 0x3c2) {
			bus.vdp.vip = 1;	// hack horrible vblank
			System.out.println();
		}
		
		cycles = 0;
		
		if (PC == 0x48f6) {
			System.out.println();
		}
		
 		GenInstruction instruction = instructions[(int) opcode];
		instruction.run((int) opcode);
		
		PC += 2;
		
		return 0;
	}
	
	public int rara() {
		long lastBit = 0;
		long maxSize = 0;
		
		long singleWordInstruction = (bus.read(PC) << 8);
		singleWordInstruction |= bus.read(PC + 1);
		
		long data = 0;
		
		System.out.println(pad4((int) PC) + " - Opcode: " + pad4((int) singleWordInstruction) + " - SR: " + pad4(SR));
		
		int opcode = (int) (singleWordInstruction >> 8);
		int register = (int) (singleWordInstruction & 0x7);
		int mode = (int) ((singleWordInstruction >> 3) & 0x7);

		int size = (int) ((singleWordInstruction >> 6) & 0x3);
		
		long operand = (bus.read(PC + 2) << 8);
		operand |= bus.read(PC + 3);
		
		//	The first extension word contains the high-order part of the address.
		int extension1 = (int) (bus.read(PC + 4) << 8);
		extension1 |= bus.read(PC + 5);
		//	The second contains the low-order part of the address.
		int extension2 = (int) (bus.read(PC + 6) << 8);
		extension2 |= bus.read(PC + 7);
		
		int contents = extension1 << 16;
		contents |= extension2;
		
		int offset = (byte) (singleWordInstruction & 0xFF);
		switch (opcode) {
		

		case 0x00:
			int destReg = register;
			int destMode = mode;
			
			Size siz = mapAlternateSize(size);
			
			data = readAddressingMode(0, siz, 0b111, 0b100, false);	// forzar que traiga #data
			
			if (siz == Size.byt) {
				lastBit = 0x80;
			} else if (siz == Size.word) {
				lastBit = 0x8000;
			} else if (siz == Size.longW) {
				lastBit = 0x8000_0000L;
			}
			
			data = D[destReg] | data;
					
			writeAddressingMode(siz, PC + 2, data, destMode, destReg);
					
			if (data == 0) {
				setZ();
			} else {
				clearZ();
			}
			if ((data & lastBit) > 0) {
				setN();
			} else {
				clearN();
			}
			
			clearV();
			clearC();
			
			break;
			
		//	ANDI		Logical AND Immediate
//			Performs a bit-wise AND operation with the immediate data and
//			the destination operand and stores the result in the destination. The
//			size of ther operation can be specified as byte, word, or long. The
//			size of the immediate data matches the operation size.
//         										   <ea>
//----------------------------------------=========================
//|15 |14 |13 |12 |11 |10 | 9 | 8 | 7 | 6 | 5 | 4 | 3 | 2 | 1 | 0 |
//|---|---|---|---|---|---|---|---|-------|-----------|-----------|
//| 0 | 0 | 0 | 0 | 0 | 0 | 1 | 0 | SIZE  |   MODE    |  REGISTER |
//|---------------------------------------------------------------|
//| 16 BITS DATA (with last Byte) |          8 BITS DATA          |
//|---------------------------------------------------------------|
//|             32 BITS DATA (included last Word)                 |
//-----------------------------------------------------------------
//			SIZE
//			00->one Byte operation
//			01->one Word operation
//			10->one Long operation
		case 0x02:
			siz = mapAlternateSize(size);
			data = readAddressingMode(siz, mode, register);
			
			if (siz == Size.byt) {
				lastBit = 0x80;
			} else if (siz == Size.word) {
				lastBit = 0x8000;
			} else if (siz == Size.longW) {
				lastBit = 0x8000_0000L;
			}

//			Logical AND Immediate to SR (privileged)
//			Performs a bit-wise AND operation with the immediate data and
//			the status register. All implemented bits of the status register are affected.
//			Size = (Word)
//			-----------------------------------------------------------------
//			|15 |14 |13 |12 |11 |10 | 9 | 8 | 7 | 6 | 5 | 4 | 3 | 2 | 1 | 0 |
//			|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|
//			| 0 | 0 | 0 | 0 | 0 | 0 | 1 | 0 | 0 | 1 | 1 | 1 | 1 | 1 | 0 | 0 |
//			|---------------------------------------------------------------|
//			|                     16 BITS IMMEDIATE DATA                    |
//			-----------------------------------------------------------------
//			X - Cleared if bit 4 of immed. operand is zero. Unchanged otherwise.
//			N - Cleared if bit 3 of immed. operand is zero. Unchanged otherwise.
//			Z - Cleared if bit 2 of immed. operand is zero. Unchanged otherwise.
//			V - Cleared if bit 1 of immed. operand is zero. Unchanged otherwise.
//			C - Cleared if bit 0 of immed. operand is zero. Unchanged otherwise.
			if ((singleWordInstruction & 0x027C) == 0x027C) {
				data = (int) (SR & operand);
				SR = (int) data;
				
				if (bitTest(data, 4)) {
					setX();
				} else {
					clearX();
				}
				if (bitTest(data, 3)) {
					setN();
				} else {
					clearN();
				}
				if (bitTest(data, 2)) {
					setZ();
				} else {
					clearZ();
				}
				if (bitTest(data, 1)) {
					setV();
				} else {
					clearV();
				}
				if (bitTest(data, 0)) {
					setC();
				} else {
					clearC();
				}
				
			} else {
				if (siz == Size.byt) {
					data = (data & operand) & 0xFF;	//	me quedo con el byte
					
					writeAddressingMode(siz, 0, data, mode, register);
					
				} else {
					throw new RuntimeException();
				}
				
				PC += 2;	// TODO ver porq si es andi.b .. el parametro ocupa 2 bytes en lugar de 1. Me parece que siempre vienen 2 bytes, aunque sea byte y se usa el segundo
				
				if (data == 0) {
					setZ();
				} else {
					clearZ();
				}
				if ((data & 0x80) > 0) {	//	TODO Mask size
					setN();
				} else {
					clearN();
				}
				clearV();
				clearC();
			}
			
			break;
		
		//	BTST
//			In the case of BTST Dn,<ea>:
//				~~~~~~~~~~~~~~~~~~~~~~~~~~~
//				-----------------------------------------------------------------
//				|15 |14 |13 |12 |11 |10 | 9 | 8 | 7 | 6 | 5 | 4 | 3 | 2 | 1 | 0 |
//				|---|---|---|---|-----------|---|---|---|-----------|-----------|
//				| 0 | 0 | 0 | 0 |  REGISTER | 1 | 0 | 0 |    MODE   | REGISTER  |
//				----------------------------------------=========================
//				                                                  <ea>
			
//			In the case of BTST #<data,<ea>:
//				~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
//				                                                  <ea>
//				----------------------------------------=========================
//				|15 |14 |13 |12 |11 |10 | 9 | 8 | 7 | 6 | 5 | 4 | 3 | 2 | 1 | 0 |
//				|---|---|---|---|---|---|---|---|---|---|-----------|-----------|
//				| 0 | 0 | 0 | 0 | 1 | 0 | 0 | 0 | 0 | 0 |    MODE   | REGISTER  |
//				|---|---|---|---|---|---|---|---|-------------------------------|
//				| 0 | 0 | 0 | 0 | 0 | 0 | 0 | 0 |       NUMBER OF THE BIT       |
//				-----------------------------------------------------------------
		case 0x01:	//	FIXME  dividir esto en metodos por cada posibilidad
		case 0x08:
			int numberBit = 0;
			destReg = register;
			destMode = mode;
			
//			 Dn *  Long only; for others modes: Byte only.
			if (destMode == 0b000) {	//	solo este modo usa long word
				siz = Size.longW;
			} else {
				siz = Size.byt;
			}
			
			if (bitTest(singleWordInstruction, 8)) {			//	BTST Dn,<ea>	<ea> is always destination
				int sourceReg = (int) ((singleWordInstruction >> 9) & 0x7);
				numberBit = (int) D[sourceReg];
				
				data = readAddressingMode(siz, destMode, destReg);
				
			} else if (bitTest(singleWordInstruction, 11)) {	//	BTST #<data,<ea>
				int sourceReg = register;
				int sourceMode = mode;
				numberBit = (int) (operand & 0xFF);
				
				data = readAddressingMode(PC + 4, siz, sourceMode, sourceReg, true);
				
				if (siz == Size.byt) {
					PC += 2;
				} else if (siz == Size.longW) {
					// nada ?
				} else {
					throw new RuntimeException("sizeee");
				}
			}
			
			if (bitTest(data, numberBit)) {
				clearZ();
			} else {
				setZ();
			}
			
			//	Bit SET
//			Tests a bit in the destination operand and sets the Z condition code
//			appropriately, then sets the bit in the destination.
//			If the destination is a data register, any of the 32 bits can be
//			specifice by the modulo 32 number. When the distination is a memory
//			location, the operation must be a byte operation, and therefore the
//			bit number is modulo 8. In all cases, bit zero is the least
//			significant bit. The bit number for this operation may be specified
//			in either of two ways:
//
//			1. Immediate -- The bit number is specified as immediate data.
//			2. Register  -- The specified data register contains the bit number.

			//			In the case of BSET Dn,<ea>:
//				~~~~~~~~~~~~~~~~~~~~~~~~~~~
//				-----------------------------------------------------------------
//				|15 |14 |13 |12 |11 |10 | 9 | 8 | 7 | 6 | 5 | 4 | 3 | 2 | 1 | 0 |
//				|---|---|---|---|-----------|---|---|---|-----------|-----------|
//				| 0 | 0 | 0 | 0 |  REGISTER | 1 | 1 | 1 |    MODE   | REGISTER  |
//				----------------------------------------=========================
//				                                                  <ea>
//
//				In the case of BSET #<data,<ea>:
//				~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
//				                                                  <ea>
//				----------------------------------------=========================
//				|15 |14 |13 |12 |11 |10 | 9 | 8 | 7 | 6 | 5 | 4 | 3 | 2 | 1 | 0 |
//				|---|---|---|---|---|---|---|---|---|---|-----------|-----------|
//				| 0 | 0 | 0 | 0 | 1 | 0 | 0 | 0 | 1 | 1 |    MODE   | REGISTER  |
//				|---|---|---|---|---|---|---|---|-------------------------------|
//				| 0 | 0 | 0 | 0 | 0 | 0 | 0 | 0 |       NUMBER OF THE BIT       |
//				-----------------------------------------------------------------
			// lo unico q cambia, es q ademas de probar el bit, lo settea
			if (((singleWordInstruction >> 6) & 0b11) == 0b11) {
				data = bitSet((int) data, numberBit);
				writeAddressingMode(siz, PC, data, destMode, destReg);
				if (siz == Size.byt) {
					PC -= 2;		//	como el source y el dest es el mismo, se sumaba 2 veces, hay q arreglarlo en el read y write addressing modes
				} else {
					throw new RuntimeException("");
				}
				
//				In the case of BCLR Dn,<ea>:
//					~~~~~~~~~~~~~~~~~~~~~~~~~~~
//					-----------------------------------------------------------------
//					|15 |14 |13 |12 |11 |10 | 9 | 8 | 7 | 6 | 5 | 4 | 3 | 2 | 1 | 0 |
//					|---|---|---|---|-----------|---|---|---|-----------|-----------|
//					| 0 | 0 | 0 | 0 |  REGISTER | 1 | 1 | 0 |    MODE   | REGISTER  |
//					----------------------------------------=========================
//					                                                  <ea>
//
//					In the case of BCLR #<data,<ea>:
//					~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
//					                                                  <ea>
//					----------------------------------------=========================
//					|15 |14 |13 |12 |11 |10 | 9 | 8 | 7 | 6 | 5 | 4 | 3 | 2 | 1 | 0 |
//					|---|---|---|---|---|---|---|---|---|---|-----------|-----------|
//					| 0 | 0 | 0 | 0 | 1 | 0 | 0 | 0 | 1 | 0 |    MODE   | REGISTER  |
//					|---|---|---|---|---|---|---|---|-------------------------------|
//					| 0 | 0 | 0 | 0 | 0 | 0 | 0 | 0 |       NUMBER OF THE BIT       |
//					-----------------------------------------------------------------
			} else if (((singleWordInstruction >> 6) & 0b10) == 0b10) {
				data = bitReset((int) data, numberBit);
				writeAddressingMode(siz, PC, data, destMode, destReg);
				if (siz == Size.byt) {
					PC -= 2;
				} else {
					throw new RuntimeException("");
				}
				
//				BCHG	Bit CHanGe
//				In the case of BCHG Dn,<ea>:
//				Tests a bit in the destination operand and sets the Z condition
//				code appropriately, then inverts the bit in the destination.
//					~~~~~~~~~~~~~~~~~~~~~~~~~~~
//					-----------------------------------------------------------------
//					|15 |14 |13 |12 |11 |10 | 9 | 8 | 7 | 6 | 5 | 4 | 3 | 2 | 1 | 0 |
//					|---|---|---|---|-----------|---|---|---|-----------|-----------|
//					| 0 | 0 | 0 | 0 |  REGISTER | 1 | 0 | 1 |    MODE   | REGISTER  |
//					----------------------------------------=========================
//					                                                  <ea>
//
//					In the case of BCHG #<data,<ea>:
//					~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
//					                                                  <ea>
//					----------------------------------------=========================
//					|15 |14 |13 |12 |11 |10 | 9 | 8 | 7 | 6 | 5 | 4 | 3 | 2 | 1 | 0 |
//					|---|---|---|---|---|---|---|---|---|---|-----------|-----------|
//					| 0 | 0 | 0 | 0 | 1 | 0 | 0 | 0 | 0 | 1 |    MODE   | REGISTER  |
//					|---|---|---|---|---|---|---|---|-------------------------------|
//					| 0 | 0 | 0 | 0 | 0 | 0 | 0 | 0 |       NUMBER OF THE BIT       |
//					-----------------------------------------------------------------
			} else if (((singleWordInstruction >> 6) & 0b10) == 0b01) {
				if (bitTest(data, numberBit)) {
					data = bitReset((int) data, numberBit);
				} else {
					data = bitSet((int) data, numberBit);
				}
				
				writeAddressingMode(siz, PC, data, destMode, destReg);
				if (siz == Size.byt) {
					PC -= 2;
				} else {
					throw new RuntimeException("");
				}
			}
			
			break;
			
		//	MOVE.b
//			-----------------------------------------------------------------
//			|15 |14 |13 |12 |11 |10 | 9 | 8 | 7 | 6 | 5 | 4 | 3 | 2 | 1 | 0 |
//			|---|---|-------|-----------|-----------|-----------|-----------|
//			| 0 | 0 |  SIZE |  REGISTER |    MODE   |    MODE   | REGISTER  |
//			----------------************************=========================
//			                    destination <ea>           source <ea>

//SIZE
//	01->Byte
//	11->Word
//	10->Long

//			X - Not affected.
//			N - Set if the result is negative. Cleared otherwise.
//			Z - Set if the result is zero. Cleared otherwise.
//			V - Always cleared.
//			C - Always cleared.
		case 0x10:
		case 0x11:
		case 0x13:
		case 0x17:
		case 0x18:
		case 0x1A:
		case 0x1C:
			int sourceReg = register;
			int sourceMode = mode;
			
			destReg = (int) ((singleWordInstruction >> 9) & 0x7);
			destMode = (int) ((singleWordInstruction >> 6) & 0x7);
			
			size = (int) ((singleWordInstruction >> 12) & 0x3);
			siz = mapSize(size);
			if (siz == Size.byt) {
				lastBit = 0x80;
			} else if (siz == Size.word) {
				lastBit = 0x8000;
			} else if (siz == Size.longW) {
				lastBit = 0x8000_0000;
			}
			
			data = readAddressingMode(siz, sourceMode, sourceReg);
			writeAddressingMode(siz, PC + 2, data, destMode, destReg);
			
			if (data == 0) {
				setZ();
			} else {
				clearZ();
			}
			if ((data & lastBit) > 0) {
				setN();
			} else {
				clearN();
			}
			clearV();
			clearC();
			
			break;
			
		//	MOVE.l		size = LONG
//			-----------------------------------------------------------------
//			|15 |14 |13 |12 |11 |10 | 9 | 8 | 7 | 6 | 5 | 4 | 3 | 2 | 1 | 0 |
//			|---|---|-------|-----------|-----------|-----------|-----------|
//			| 0 | 0 |  SIZE |  REGISTER |    MODE   |    MODE   | REGISTER  |
//			----------------************************=========================
//			                    destination <ea>           source <ea>
//		Size
//			01->Byte
//			11->Word
//			10->Long
		case 0x20:
		case 0x21:
		case 0x22:
		case 0x23:
		case 0x24:
		case 0x26:
		case 0x28:
		case 0x2A:
		case 0x2D:
		case 0x2E:
			size = (int) ((singleWordInstruction >> 12) & 0x3);
			siz = mapSize(size);
			data = readAddressingMode(siz, mode, register);
			
			int mask = 0;
			if (size == 0b01) {
				mask = 0x80;
 			} else if (size == 0b11) {
 				mask = 0x8000;
 			} else if (size == 0b10) {
 				mask = 0x8000_0000;
 			}
			
			destMode = (int) ((singleWordInstruction >> 6) & 0x7);
			destReg = (int) ((singleWordInstruction >> 9) & 0x7);
			
			writeAddressingMode(siz, PC + 2, data, destMode, destReg);
			
			if (data == 0) {
				setZ();
			} else {
				clearZ();
			}
			if ((data & mask) > 0) {
				setN();
			} else {
				clearN();
			}
			clearV();
			clearC();
			
			break;
			
		//	MOVEA	Move Address Source -> Destination	Size = (Word, Long)
//			Move the contents of the source to the destination address register.
//			Word sized operands are sign extended to 32 bits before the operation is done.
//			-----------------------------------------------------------------
//			|15 |14 |13 |12 |11 |10 | 9 | 8 | 7 | 6 | 5 | 4 | 3 | 2 | 1 | 0 |
//			|---|---|-------|-----------|---|---|---|-----------|-----------|
//			| 0 | 0 |  SIZE |  ADDRESS  | 0 | 0 | 1 |    MODE   | REGISTER  |
//			|   |   |       |  REGISTER |   |   |   |           |           |
//			----------------------------------------=========================
//			                                              source <ea>
//			SIZE
//			11->Word, 32 bits of address register are altered by sign extension.
//			10->Long
		case 0x2C:
			size = (int) ((singleWordInstruction >> 12) & 0x3);
			destReg = (int) ((singleWordInstruction >> 9) & 0x7);

			siz = mapSize(size);
			data = readAddressingMode(siz, mode, register);
			
			if (siz == Size.word) {
				if ((data & 0x8000) > 0) {
					data |= 0xFFFF_0000;
				}
			}
			A[destReg] = data;
			
			break;
			
		//	MOVE.w
		case 0x30:
		case 0x32:
		case 0x33:
		case 0x34:
		case 0x36:
		case 0x38:
		case 0x3A:
		case 0x3C:
		case 0x3E:
			sourceReg = register;
			sourceMode = mode;
			
			destReg = (int) ((singleWordInstruction >> 9) & 0x7);
			destMode = (int) ((singleWordInstruction >> 6) & 0x7);
			
			size = (int) ((singleWordInstruction >> 12) & 0x3);
			siz = mapSize(size);
			data = readAddressingMode(siz, sourceMode, sourceReg);
			
			writeAddressingMode(siz, PC + 2, data, destMode, destReg);
			
			if (data == 0) {
				setZ();
			} else {
				clearZ();
			}
			if ((data & 0x8000) > 0) {
				setN();
			} else {
				clearN();
			}
			clearV();
			clearC();
			
			break;
		
//		//	MOVE to SR -- Move to status register (PRIVILEGED)
//			-----------------------------------------------------------------
//			|15 |14 |13 |12 |11 |10 | 9 | 8 | 7 | 6 | 5 | 4 | 3 | 2 | 1 | 0 |
//			|---|---|---|---|---|---|---|---|---|---|-----------|-----------|
//			| 0 | 1 | 0 | 0 | 0 | 1 | 1 | 0 | 1 | 1 |    MODE   | REGISTER  |
//			----------------------------------------=========================
//			                                                  <ea>
//			X - Set the same as bit 4 of the source operand.
//			N - Set the same as bit 3 of the source operand.
//			Z - Set the same as bit 2 of the source operand.
//			V - Set the same as bit 1 of the source operand.
//			C - Set the same as bit 0 of the source operand.
		case 0x46:
			siz = mapSize(size);
			data = readAddressingMode(siz, mode, register);
			
			SR = (int) data;
			
			if (bitTest(data, 4)) {
				setX();
			} else {
				clearX();
			}
			if (bitTest(data, 3)) {
				setN();
			} else {
				clearN();
			}
			if (bitTest(data, 2)) {
				setZ();
			} else {
				clearZ();
			}
			if (bitTest(data, 1)) {
				setV();
			} else {
				clearV();
			}
			if (bitTest(data, 0)) {
				setC();
			} else {
				clearC();
			}
			
			break;
			
		//	TST
//			-----------------------------------------------------------------
//			|15 |14 |13 |12 |11 |10 | 9 | 8 | 7 | 6 | 5 | 4 | 3 | 2 | 1 | 0 |
//			|---|---|---|---|---|---|---|---|-------|-----------|-----------|
//			| 0 | 1 | 0 | 0 | 1 | 0 | 1 | 0 | SIZE  |   MODE    |  REGISTER |
//			----------------------------------------=========================
//		                                                          <ea>
//SIZE
//	00->one Byte operation
//	01->one Word operation
//	10->one Long operation
//		RESULT
//			X - Not affected.
//			N - Set if the result is negative. Cleared otherwise.
//			Z - Set if the result is zero. Cleared otherwise.
//			V - Always cleared.
//			C - Always cleared.
		case 0x4A:
			siz = mapAlternateSize(size);
			data = readAddressingMode(siz, mode, register);
			
			if (siz == Size.byt)	{	//	byte
				lastBit = 0x80;
			} else if (siz == Size.word) {	//	word
				lastBit = 0x8000;
			} else if (siz == Size.longW) {	//	long
				lastBit = 0x8000_0000;
			}
			
			if (data == 0) {
				setZ();
			} else {
				clearZ();
			}
			if ((data & lastBit) > 0) {
				setN();
			} else {
				clearN();
			}
			
			clearV();
			clearC();
			
			break;
			
		// LEA	(Load effective address)
//			Places the specified address into the destination address
//			register. Note: All 32 bits of An are affected by this instruction.
//			-----------------------------------------------------------------
//			|15 |14 |13 |12 |11 |10 | 9 | 8 | 7 | 6 | 5 | 4 | 3 | 2 | 1 | 0 |
//			|---|---|---|---|-----------|---|---|---|-----------|-----------|
//			| 0 | 1 | 0 | 0 | REGISTER  | 1 | 1 | 1 |    MODE   | REGISTER  |
//			----------------------------------------=========================
//			                                                   <ea>
//			Size = (Long)
		case 0x41:
		case 0x43:
		case 0x45:
		case 0x47:
		case 0x49:
		case 0x4B:
		case 0x4D:
		case 0x4F:
			destReg = (int) ((singleWordInstruction >> 9) & 0x7);
			
			data = readAddressingMode(PC + 2, Size.longW, mode, register, false);
			
			A[destReg] = data;
			
			break;
			
		//	CLR		CLeaR
//			Clears the destination operand to zero.
//
//			On an MC68000 and MC68HC000, a CLR instruction does both a
//			read and a write to the destination. Because of this, this
//			instruction should never be used on custom chip registers.
//			-----------------------------------------------------------------
//			|15 |14 |13 |12 |11 |10 | 9 | 8 | 7 | 6 | 5 | 4 | 3 | 2 | 1 | 0 |
//			|---|---|---|---|---|---|---|---|-------|-----------|-----------|
//			| 0 | 1 | 0 | 0 | 0 | 0 | 1 | 0 |  SIZE |    MODE   | REGISTER  |
//			----------------------------------------=========================
//			                                                  <ea>
//			SIZE
//			00->one Byte operation
//			01->one Word operation
//			10->one Long operation
//
//		RESULT
//			X - Not affected
//			N - Always cleared
//			Z - Always set
//			V - Always cleared
//			C - Always cleared
		case 0x42:
			size = (int) ((singleWordInstruction >> 6) & 0x3);
			destReg = register;
			destMode = mode;
			
			siz = mapAlternateSize(size);
			
			data = 0;
			
			writeAddressingMode(siz, PC + 2, data, destMode, destReg);
			
			clearN();
			setZ();
			clearV();
			clearC();
			
			break;
			
		//	SWAP		SWAP register upper and lower words
//			Swaps between 16 low bits and 16 high bits of register.
//			-----------------------------------------------------------------
//			|15 |14 |13 |12 |11 |10 | 9 | 8 | 7 | 6 | 5 | 4 | 3 | 2 | 1 | 0 |
//			|---|---|---|---|---|---|---|---|---|---|---|---|---|-----------|
//			| 0 | 1 | 0 | 0 | 1 | 0 | 0 | 0 | 0 | 1 | 0 | 0 | 0 | REGISTER  |
//			-----------------------------------------------------------------
//			"REGISTER" indicates the number of register on which swap is made.

//			X - Not affected
//			N - Set if the most-significant bit of the result was set. Cleared otherwise.
//			Z - Set if the 32 bits result was zero. Cleared otherwise.
//			V - Always cleared.
//			C - Always cleared.
		case 0x48:
			data = D[register];
			data = ((data & 0xFFFF_0000L) >> 16) | ((data & 0x0000_FFFFL) << 16);
			D[register] = data;
			
			if (data == 0) {
				setZ();
			} else {
				clearZ();
			}
			if ((data & 0x8000_0000L) > 0) {
				setN();
			} else {
				clearN();
			}
			clearV();
			clearC();
			
			break;
			
		//	MOVEM	Move Multiple Registers
		//	dr field—Specifies the direction of the transfer.
			//	0 — Register to memory.
			//	1 — Memory to register.
			//	size:	0 — Word transfer 1 — Long transfer 
//					-----------------------------------------=========================
//					|15 |14 |13 |12 |11 |10 | 9 | 8 | 7 | 6  | 5 | 4 | 3 | 2 | 1 | 0 |
//					|---|---|---|---|---|---|---|---|---|----|-----------|-----------|
//					| 0 | 1 | 0 | 0 | 1 |dr | 0 | 0 | 1 |SIZE|    MODE   | REGISTER  |
//					|----------------------------------------------------------------|
//					|                    MASK FROM REGISTER LIST                     |
//					------------------------------------------------------------------
		case 0x4C:
			boolean dr = bitTest(singleWordInstruction, 10);
			boolean sizes = bitTest(singleWordInstruction, 6);
			
			int registerListMaskA = (int) bus.read(PC + 2);	// TODO ojo q con pre decrement es al reves la interpretacion
			int registerListMaskD = (int) bus.read(PC + 3);
			
			if (dr) {	//	memory to register
				if (sizes) {	// long transfer
					if (mode == 0b010) {	//	(An)
						long desvio = A(register);
						desvio = copyMask(registerListMaskD, registerListMaskA, desvio);
						
					} else if (mode == 0b011) {		//	(An)+	 Address Register Indirect with Postincrement Mode 
						long desvio = A(register);
						desvio = copyMask(registerListMaskD, registerListMaskA, desvio);
						
						A[register] = desvio;	// post increment long, 4 por cada copia
						
					} else {
						throw new RuntimeException("not impl otros addressings modes " + mode);
					}
					
				} else {
					throw new RuntimeException("IMPLE WORD");
				}
			} else {
				throw new RuntimeException("IMPLE register to mem");
			}
			
			PC += 2;
			
			break;
			
		//	Move to/from USP (privileged)
//			-----------------------------------------------------------------
//			|15 |14 |13 |12 |11 |10 | 9 | 8 | 7 | 6 | 5 | 4 | 3 | 2 | 1 | 0 |
//			|---|---|---|---|---|---|---|---|---|---|---|---|---|-----------|
//			| 0 | 1 | 0 | 0 | 1 | 1 | 1 | 0 | 0 | 1 | 1 | 0 |dr | REGISTER  |
//			-----------------------------------------------------------------
		case 0x4E:
			if ((singleWordInstruction & 0xFFF8) == 0x4E60) {	//	match mask
//				dr specifies move direction:
//				0->An to USP
//				1->USP to An
				dr = bitTest(singleWordInstruction, 3);
				if (!dr) {
					USP = A[register];
				} else {
					A[register] = USP;
				}
				
			} else if ((singleWordInstruction & 0xFFFF) == 0x4E75) {
//				RTS		ReTurn from Subroutine
//				-----------------------------------------------------------------
//				|15 |14 |13 |12 |11 |10 | 9 | 8 | 7 | 6 | 5 | 4 | 3 | 2 | 1 | 0 |
//				|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|
//				| 0 | 1 | 0 | 0 | 1 | 1 | 1 | 0 | 0 | 1 | 1 | 1 | 0 | 1 | 0 | 1 |
//				-----------------------------------------------------------------

				long newPC = 0;
				
				newPC |= (bus.read(SSP) << 24);
				SSP++;
				newPC |= (bus.read(SSP) << 16);
				SSP++;
				newPC |= (bus.read(SSP) << 8);
				SSP++;
				newPC |= (bus.read(SSP) << 0);
				SSP++;
				
				PC = newPC - 2;	// -2 porque despues le agrega +2

			} else if ((singleWordInstruction & 0xFFC0) == 0x4E80) {
//				JSR		Jump to SubRoutine
//				Pushes the long word address of the instruction immediately
//				following the JSR instruction onto the stack. The PC contains
//				the address of the instruction word plus two. Program execution
//				continues at location specified by <ea>.
//
//			FORMAT
//				-----------------------------------------------------------------
//				|15 |14 |13 |12 |11 |10 | 9 | 8 | 7 | 6 | 5 | 4 | 3 | 2 | 1 | 0 |
//				|---|---|---|---|---|---|---|---|---|---|-----------|-----------|
//				| 0 | 1 | 0 | 0 | 1 | 1 | 1 | 0 | 1 | 0 |    MODE   | REGISTER  |
//				----------------------------------------=========================
//				                                                   <ea>
				destReg = register;
				destMode = mode;
				
				long addr = readAddressingMode(PC + 2, Size.word, destMode, destReg, false);
				
				long oldPC = PC + 2;
				
				SSP--;
				bus.write(SSP, oldPC & 0xFF, Size.byt);
				SSP--;
				bus.write(SSP, (oldPC >> 8) & 0xFF, Size.byt);
				SSP--;
				bus.write(SSP, (oldPC >> 16) & 0xFF, Size.byt);
				SSP--;
				bus.write(SSP, (oldPC >> 24), Size.byt);
				
				PC = (addr - 2) & 0xFFFFFF;
				
			} else if ((singleWordInstruction & 0xFFFF) == 0x4E71) {
			//	NOP
//			Nothing happens! This instruction will basically wait until
//			all pending bus activity is completed. This allows
//			synchronization of the pipeline	and prevents instruction overlap.
//
//		FORMAT
//			-----------------------------------------------------------------
//			|15 |14 |13 |12 |11 |10 | 9 | 8 | 7 | 6 | 5 | 4 | 3 | 2 | 1 | 0 |
//			|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|
//			| 0 | 1 | 0 | 0 | 1 | 1 | 1 | 0 | 0 | 1 | 1 | 1 | 0 | 0 | 0 | 1 |
//			-----------------------------------------------------------------
			
				//	TODO esperar al bus de z80 ? implementarlo mejor
				bus.z80.reset = false;
				
			} else {
				
				throw new RuntimeException("4E no impl");
			}
			break;
			
		//	DBcc	Decrement and Branch Conditionally -	Size of offset = (Word)
//			-----------------------------------------------------------------
//			|15 |14 |13 |12 |11 |10 | 9 | 8 | 7 | 6 | 5 | 4 | 3 | 2 | 1 | 0 |
//			|---|---|---|---|---------------|---|---|---|---|---|-----------|
//			| 0 | 1 | 0 | 1 |   CONDITION   | 1 | 1 | 0 | 0 | 1 | REGISTER  |
//			|---------------------------------------------------------------|
//			|                      16 BITS OFFSET (d16)                     |
//			-----------------------------------------------------------------
		case 0x51:
			offset = (int) operand;
			int counter = (int) ((D[register]) & 0xFFFF);	//	solo la word
			branch(counter != 0, 0, (int) operand);	// 0 es word, siempre es word esta instruccion
			
			counter = (counter - 1) & 0xFFFF;
			D[register] = (D[register] & 0xFFFF_0000) | counter;	// la word alta permanece, la baja es el counter
			
			break;
			
		//	ADDQ.l	ADD 3-bit immediate Quick
//			Adds the immediate value of 1 to 8 to the operand at the
//			destination location. The size of the operation may be specified as
//			byte, word, or long. When adding to address registers, the condition
//			codes are not altered, and the entire destination address register is
//			used regardless of the operation size.
//			-----------------------------------------------------------------
//			|15 |14 |13 |12 |11 |10 | 9 | 8 | 7 | 6 | 5 | 4 | 3 | 2 | 1 | 0 |
//			|---|---|---|---|-----------|---|-------|-----------|-----------|
//			| 0 | 1 | 0 | 1 |    DATA   | 0 | SIZE  |    MODE   |  REGISTER |
//			----------------------------------------=========================
//		                                                          <ea>
//			DATA
//			000        ->represent value 8
//			001 to 111 ->immediate data from 1 to 7
//
//		SIZE
//			00->one Byte operation
//			01->one Word operation
//			10->one Long operation
			
//			X - Set the same as the carry bit.
//			N - Set if the result is negative. Cleared otherwise.
//			Z - Set if the result is zero. Cleared otherwise.
//			V - Set if an overflow is generated. Cleared otherwise.
//			C - Set if a carry is generated. Cleared otherwise.
		case 0x52:
			int dataAdd = (int) ((singleWordInstruction >> 9) & 0x7);
			
			size = (int) ((singleWordInstruction >> 6) & 0x3);
			siz = mapAlternateSize(size);
			data = readAddressingMode(siz, mode, register);
			if (data == 0) {
				data = 8;
			}
			
			long result = 0;
			
			if (size == 0b10) {		// Long operation  FIXME, usar un enum para sizes, porque estos sizes no coinciden con los de mi metodo de addressing modes
				result = data + dataAdd;
				if (mode == 0b000) {
					D[register] = (result & 0xFFFF_FFFFL);
				} else {
					throw new RuntimeException("modes !");
				}
			} else {
				throw new RuntimeException("sizes !");
			}
			
			//TODO implementar otros sizes
			if ((result & 0xFFFF_FFFFL) == 0) {
				setZ();
			} else {
				clearZ();
			}
			if (result > 0xFFFF_FFFFL) {
				setC();
				setX();
			} else {
				clearC();
				clearX();
			}
			if ((result & 0x8000_0000L) > 0) {
				setZ();
			} else {
				clearZ();
			}
			// TODO implementar OVERFLOW
			
			break;
			
//		//	SUBQ	SUBtract 3-bit immediate Quick
//			Subtracts the immediate value of 1 to 8 to the operand at the
//			destination location. The size of the operation may be specified as
//			byte, word, or long. When subtracting to address registers,
//		        the condition codes are not altered, and the entire destination
//		        address register is used regardless of the operation size.
//			-----------------------------------------------------------------
//			|15 |14 |13 |12 |11 |10 | 9 | 8 | 7 | 6 | 5 | 4 | 3 | 2 | 1 | 0 |
//			|---|---|---|---|-----------|---|-------|-----------|-----------|
//			| 0 | 1 | 0 | 1 |    DATA   | 1 | SIZE  |    MODE   |  REGISTER |
//			----------------------------------------=========================
//		                                                          <ea>
//			SIZE
//			00->one Byte operation
//			01->one Word operation
//			10->one Long operation

//			X - Set the same as the carry bit.
//			N - Set if the result is negative. Cleared otherwise.
//			Z - Set if the result is zero. Cleared otherwise.
//			V - Set if an overflow is generated. Cleared otherwise.
//			C - Set if a carry is generated. Cleared otherwise.

		case 0x53:
			int dataSub = (int) ((singleWordInstruction >> 9) & 0x7);
			
			size = (int) ((singleWordInstruction >> 6) & 0x3);
			siz = mapAlternateSize(size);
			data = readAddressingMode(siz, mode, register);
			if (data == 0) {
				data = 8;
			}
			
			result = 0;
			
			if (size == 0b10) {		// Long operation  FIXME, usar un enum para sizes, porque estos sizes no coinciden con los de mi metodo de addressing modes
				result = data - dataSub;
				if (mode == 0b000) {
					D[register] = (result & 0xFFFF_FFFFL);
					
				} else {
					throw new RuntimeException("modes !");
				}
				
			} else if (size == 0b01) {	//	01->one Word operation
				result = data - dataSub;
				if (mode == 0b000) {
					D[register] = (D[register] & 0xFFFF_0000) | (result & 0xFFFF);
					
				} else {
					throw new RuntimeException("modes !");
				}
				
			} else {
				throw new RuntimeException("sizes !");
			}
			
			//TODO implementar otros sizes
			if ((result & 0xFFFF_FFFFL) == 0) {
				setZ();
			} else {
				clearZ();
			}
			if ((result & 0x8000_0000L) > 0) {
				setN();
			} else {
				clearN();
			}
			if (result > 0xFFFF_FFFFL) {
				setC();
				setX();
			} else {
				clearC();
				clearX();
			}
			if ((result & 0x8000_0000L) > 0) {
				setZ();
			} else {
				clearZ();
			}
			// TODO implementar OVERFLOW
			
			break;
			
		//	Unconditional BRAnch
//			Program execution continues at location (PC) + offset.
//			Offset is the relative gap between PC value at BRA ((PC) + 2)
//			instruction execution time and mentioned label.
//			Size = (Byte, Word)
//			-----------------------------------------------------------------
//			|15 |14 |13 |12 |11 |10 | 9 | 8 | 7 | 6 | 5 | 4 | 3 | 2 | 1 | 0 |
//			|---|---|---|---|---|---|---|---|-------------------------------|
//			| 0 | 1 | 1 | 0 | 0 | 0 | 0 | 0 |         8 BITS OFFSET         |
//			|---------------------------------------------------------------|
//			|            16 BITS OFFSET, IF 8 BITS OFFSET = $00             |
//			|---------------------------------------------------------------|
//			|            32 BITS OFFSET, IF 8 BITS OFFSET = $FF             |
//			-----------------------------------------------------------------
		case 0x60:
			branch(true, offset, (int) operand);
			
			break;
			
		//	Branch to SubRoutine
//			Size = (Byte, Word)
//			Pushes the long word address which follows the BSR instruction to stack.
//			Program	execution continues at location (PC) + offset.
//			Offset is the relative gap between PC value and label.
//			This gap is calculated by complement to two and is coded on 8 bits
//			or on 16 bits.
//			-----------------------------------------------------------------
//			|15 |14 |13 |12 |11 |10 | 9 | 8 | 7 | 6 | 5 | 4 | 3 | 2 | 1 | 0 |
//			|---|---|---|---|---|---|---|---|-------------------------------|
//			| 0 | 1 | 1 | 0 | 0 | 0 | 0 | 1 |         8 BITS OFFSET         |
//			|---------------------------------------------------------------|
//			|            16 BITS OFFSET, IF 8 BITS OFFSET = $00             |
//			|---------------------------------------------------------------|
//			|            32 BITS OFFSET, IF 8 BITS OFFSET = $FF             |
//			-----------------------------------------------------------------
		case 0x61:
			long oldPC = 0;
			if ((singleWordInstruction & 0xFF) == 0b00) {	//	word
				oldPC = PC + 4;
			} else {	//	byte	(long word no hay en este cpu para esta instr)
				offset = (int) (singleWordInstruction & 0xFF);
				oldPC = PC + 2;
			}
			
			branch(true, offset, (int) operand);
			
			SSP--;
			bus.write(SSP, oldPC & 0xFF, Size.byt);
			SSP--;
			bus.write(SSP, (oldPC >> 8) & 0xFF, Size.byt);
			SSP--;
			bus.write(SSP, (oldPC >> 16) & 0xFF, Size.byt);
			SSP--;
			bus.write(SSP, (oldPC >> 24), Size.byt);
			
			break;
			
		//	Bcc - BNE
		case 0x66:
			boolean taken = !isZ();
			branch(taken, offset, (int) operand);
			
			break;
			
		//	Bcc - BQE
		case 0x67:
			taken = isZ();
			branch(taken, offset, (int) operand);
			
			break;
		
		//	MOVEQ	MOVE signed 8-bit data Quick	Size = (Long)
//			-----------------------------------------------------------------
//			|15 |14 |13 |12 |11 |10 | 9 | 8 | 7 | 6 | 5 | 4 | 3 | 2 | 1 | 0 |
//			|---|---|---|---|-----------|---|-------------------------------|
//			| 0 | 1 | 1 | 1 | REGISTER  | 0 |        IMMEDIATE DATA         |
//			-----------------------------------------------------------------
		case 0x70:
		case 0x72:
		case 0x74:
		case 0x76:
		case 0x78:
		case 0x7A:
		case 0x7C:
			destReg = (int) ((singleWordInstruction >> 9) & 0x7);
			data = singleWordInstruction & 0xFF;
			
			//	The specified data is sign extended to 32-bits before it is moved to the register.
			if ((data & 0x80) > 0) {
				data |= 0xFFFF_FF00L;	// sign extend 32 bits
			}
			D[destReg] = data;
			
			if (data == 0) {
				setZ();
			} else {
				clearZ();
			}
			if ((data & 0x80) > 0) {	//	FIXME, me parece q si es un registro entero, deberia ser mas grande la mask
				setN();
			} else {
				clearN();
			}
			
			clearV();
			clearC();
			
			break;

		//	SUB		SUBtract
//			Subtracts source operand to destination operand.
//	        Result is stored to destination's place.
//			-----------------------------------------------------------------
//			|15 |14 |13 |12 |11 |10 | 9 | 8 | 7 | 6 | 5 | 4 | 3 | 2 | 1 | 0 |
//			|---|---|---|---|-----------|-----------|-----------|-----------|
//			| 1 | 0 | 0 | 1 |  REGISTER |  OP-MODE  |    MODE   |  REGISTER |
//			----------------------------------------=========================
//		                                                          <ea>
//			OP-MODE
//			Byte	Word	Long
//			~~~~	~~~~	~~~~
//			000		001		010	(Dn) - (<ea>) -> Dn
//			100		101		110	(<ea>) - (Dn) -> <ea>
			
//			X - Set the same as the carry bit.
//			N - Set if the result is negative. Cleared otherwise.
//			Z - Set if the result is zero. Cleared otherwise.
//			V - Set if an overflow is generated. Cleared otherwise.
//			C - Set if a carry is generated. Cleared otherwise.
		case 0x90:
			long tmp = 0;
			int reg = (int) ((singleWordInstruction >> 9) & 0x7);
			int opMode = (int) ((singleWordInstruction >> 6) & 0x7);
			
			siz = mapFromOpMode(size);
			
			data = readAddressingMode(siz, mode, register);		// TODO segun si es destino o source, distintos addressing modes son permitidos, refactor del metodo para poder aceptar segun el opcode, que modes son permitidos y cuales no
			if (opMode == 0b001) {	// word
				tmp = (D[reg] - data);
				D[reg] = ((D[reg] & 0xFFFF_0000) | tmp) & 0xFFFF_FFFF;
				
				lastBit = 0x8000;
				maxSize = 0xFFFF;
			} else if (opMode == 0b010) {	//	(Dn) - (<ea>) -> Dn
				tmp = (D[reg] - data);
				D[reg] = tmp & 0xFFFF_FFFF;
				
				lastBit = 0x8000_0000L;
				maxSize = 0xFFFF_FFFFL;
			
			} else {
				throw new RuntimeException("NOT IMPL");
			}
			
			if (tmp == 0) {
				setZ();
			} else {
				clearZ();
			}
			if ((tmp & lastBit) > 0) {
				setN();
			} else {
				clearN();
			}
			if (tmp > maxSize) {
				setC();
				clearX();
			} else {
				clearC();
				setX();
			}
			// TODO calcular overflow !
			
			break;
		
		//	CMP
//			Subtracts the source operand from the destination data register and
//			sets the condition codes according to the result. The data register is NOT changed.
//			-----------------------------------------------------------------
//			|15 |14 |13 |12 |11 |10 | 9 | 8 | 7 | 6 | 5 | 4 | 3 | 2 | 1 | 0 |
//			|---|---|---|---|-----------|-----------|-----------|-----------|
//			| 1 | 0 | 1 | 1 |  REGISTER |  OP-MODE  |    MODE   | REGISTER  |
//			----------------------------------------=========================
//			                                                  <ea>

//OP-MODE
//	000	8 bits operation.
//	001	16 bits operation.
//	010	32 bits operation.
			
//			X - Not affected
//			N - Set if the result is negative. Cleared otherwise.
//			Z - Set if the result is zero. Cleared otherwise.
//			V - Set if an overflow is generated. Cleared otherwise.
//			C - Set if a carry is generated. Cleared otherwise.
		case 0xB2:
			destReg = (int) ((singleWordInstruction >> 9) & 0x7);
			opMode = (int) ((singleWordInstruction >> 6) & 0x7);
			tmp = 0;
			if (opMode == 0b001) {	//	16 bits operation.
				data = readAddressingMode(Size.word, mode, register);
				int sub = (int) (D[destReg] & 0xFFFF);
				tmp = data - sub;
				
				mask = 0x8000;
				maxSize = 0xFFFF;
				
			} else {
				throw new RuntimeException("OPMODE !");
			}
			
			if ((tmp & maxSize) == 0) {
				setZ();
			} else {
				clearZ();
			}
			if ((tmp & mask) > 0) {
				setN();
			} else {
				clearN();
			}
			if (tmp > maxSize) {	// TODO revisar este carry, nunca puede pasarse, < 0 ?
				setC();
			} else {
				clearC();
			}
			
			// TODO overflow
			
			break;
			
		//	ADD		Size = (Byte, Word, Long)
//			Byte	Word	Long
//			~~~~	~~~~	~~~~
//			000		001		010	(Dn) + (<ea>) -> Dn
//			100		101		110	(<ea>) + (Dn) -> <ea>
//			-----------------------------------------------------------------
//			|15 |14 |13 |12 |11 |10 | 9 | 8 | 7 | 6 | 5 | 4 | 3 | 2 | 1 | 0 |
//			|---|---|---|---|-----------|-----------|-----------|-----------|
//			| 1 | 1 | 0 | 1 |  REGISTER |  OP-MODE  |    MODE   |  REGISTER |
//			----------------------------------------=========================
//			 												   <ea>
		case 0xD2:
		case 0xDA:
			long tot = 0;
			lastBit = 0;
			maxSize = 0;
			reg = (int) ((singleWordInstruction >> 9) & 0x7);
			opMode = (int) ((singleWordInstruction >> 6) & 0x7);
			siz = mapFromOpMode(size);
			if (opMode == 0b000 || opMode == 0b001 || opMode == 0b010) {	// (Dn) + (<ea>) -> Dn
				if (siz == Size.word) {
					data = readAddressingMode(Size.word, mode, register);		// TODO segun si es destino o source, distintos addressing modes son permitidos, refactor del metodo para poder aceptar segun el opcode, que modes son permitidos y cuales no
					tot = (D[reg] + data);
					D[reg] = ((D[reg] & 0xFFFF_0000) | (tot & 0x0000_FFFFL)) & 0xFFFF_FFFF;
					
					lastBit = 0x8000;
					maxSize = 0xFFFF;
				} else {
					throw new RuntimeException("NOT IMPL");
				}
			} else {	//	(<ea>) + (Dn) -> <ea>
				throw new RuntimeException("NOT IMPL");
			}
			
			if (tot == 0) {
				setZ();
			} else {
				clearZ();
			}
			if ((tot & lastBit) > 0) {
				setN();
			} else {
				clearN();
			}
			if (tot > maxSize) {
				setC();
				clearX();
			} else {
				clearC();
				setX();
			}
			// TODO calcular overflow !
			
			break;
			
		//	LSL		Logical Shift Left
//			Shift the bits of the operand in the specified direction.
//			The carry bit set set to the last bit shifted out of the operand.
//			The shift count for the shifting of a register may be specified in
//			two different ways:
//
//			1. Immediate - the shift count is specified in the instruction (shift
//			               range 1-8).
//			2. Register  - the shift count is contained in a data register
//			               specified in the instruction (shift count mod 64)
//
//			For a register, the size may be byte, word, or long, but for a memory
//			location, the size must be a word. The shift count is also restricted
//			to one for a memory location.
			
//			In the case of the shifting of a register:
//				~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
//				-----------------------------------------------------------------
//				|15 |14 |13 |12 |11 |10 | 9 | 8 | 7 | 6 | 5 | 4 | 3 | 2 | 1 | 0 |
//				|---|---|---|---|-----------|---|-------|---|---|---|-----------|
//				| 1 | 1 | 1 | 0 |  NUMBER/  |dr |  SIZE |i/r| 0 | 1 | REGISTER  |
//				|   |   |   |   |  REGISTER |   |       |   |   |   |           |
//				-----------------------------------------------------------------
//
//				In the case of the shifting of a memory area:
//				~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
//				-----------------------------------------------------------------
//				|15 |14 |13 |12 |11 |10 | 9 | 8 | 7 | 6 | 5 | 4 | 3 | 2 | 1 | 0 |
//				|---|---|---|---|---|---|---|---|---|---|-----------|-----------|
//				| 1 | 1 | 1 | 0 | 0 | 0 | 1 |dr | 1 | 1 |    MODE   | REGISTER  |
//				----------------------------------------=========================
//				                                                  <ea>
			
//			NUMBER/REGISTER
//			Specifies number of shifting or number of register which contents
//			the number of shifting.
//			If i/r = 0, number of shifting is specified in the instruction as
//			immediate data
//			If i/r = 1, it's specified in the data register.
//			If dr = 0, right shifting
//			If dr = 1, left shifting

//			SIZE
//			00->one Byte operation
//			01->one Word operation
//			10->one Long operation
			
//			X - Set according to the last bit shifted out of the operand.
//			N - Set if the result is negative. Cleared otherwise.
//			Z - Set if the result is zero. Cleared otherwise.
//			V - Always cleared
//			C - Set according to the last bit shifted out of the operand.
		case 0xE1:
			size = (int) ((singleWordInstruction >> 6) & 0x3);
			boolean ir = bitTest(singleWordInstruction, 5);
			dr = bitTest(singleWordInstruction, 8);
			int numberRegister = (int) ((singleWordInstruction >> 9) & 0x7);
			
			if (size == 0b11) {		// memory shifting
				throw new RuntimeException("NOT !");
				
			} else {	//	register shifting
				data = D[register];
				
				if (!ir) {
					int shift = numberRegister;
					if (shift == 0) {
						shift = 8;
					}
					
					if (!dr) {
						data = data >> shift;
					} else {
						data = (data << shift) & 0xFFFF_FFFFL;
					}
					D[register] = data;
					
				} else {
					throw new RuntimeException("NOT !");
				}
			}
			
			if (data == 0) {
				setZ();
			} else {
				clearZ();
			}
			if ((data & 0x8000_0000L) > 0) {	// TODO otros sizes ?
				setN();
			} else {
				clearN();
			}
			
			// TODO impl carry y X y Overflow
			
			break;
			
		//	ASR		Arithmetic Shift Right
//			Performs an arithmetic shifting bit operation in the indicated
//	        direction, with an immediate data, or with a data register.
//	        If you shift address contents, you only can do ONE shift, and
//	        your operand is ONE word exclusively.
//			ASR:      -->
//			  .---> OPERAND ------> C
//		      |    T          |
//			  |    |          |
//			  `----'          `---> X
//		In the case of the shifting of a register:
//			~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
//			-----------------------------------------------------------------
//			|15 |14 |13 |12 |11 |10 | 9 | 8 | 7 | 6 | 5 | 4 | 3 | 2 | 1 | 0 |
//			|---|---|---|---|-----------|---|-------|---|---|---|-----------|
//			| 1 | 1 | 1 | 0 |  NUMBER/  |dr |  SIZE |i/r| 0 | 0 | REGISTER  |
//			|   |   |   |   |  REGISTER |   |       |   |   |   |           |
//			-----------------------------------------------------------------
//		In the case of the shifting of a memory area:
//			~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
//			-----------------------------------------------------------------
//			|15 |14 |13 |12 |11 |10 | 9 | 8 | 7 | 6 | 5 | 4 | 3 | 2 | 1 | 0 |
//			|---|---|---|---|---|---|---|---|---|---|-----------|-----------|
//			| 1 | 1 | 1 | 0 | 0 | 0 | 0 |dr | 1 | 1 |    MODE   | REGISTER  |
//			----------------------------------------=========================
//			                                                  <ea>
//			NUMBER/REGISTER
//			Specifies number of shifting or number of register which contents the number of shifting.
//			If i/r = 0, number of shifting is specified in the instruction as immediate data
//			If i/r = 1, it's specified in the data register.
//			If dr = 0, right shifting
//			If dr = 1, left shifting
//
//		SIZE
//			00->one Byte operation
//			01->one Word operation
//			10->one Long operation
//
//		REGISTER
//			For a register shifting:
//			Indicates the number of data register on which shifting is applied.
			
//		X - Set according to the list bit shifted out of the operand.
//		    Unaffected for a shift count of zero.
//		N - Set if the most-significant bit of the result is set. Cleared
//		    otherwise.
//		Z - Set if the result is zero. Cleared otherwise.
//		V - Set if the most significant bit is changed at any time during
//		    the shift operation. Cleared otherwise.
//		C - Set according to the list bit shifted out of the operand.
//		    Cleared for a shift count of zero.

		case 0xE2:
			size = (int) ((singleWordInstruction >> 6) & 0x3);
			ir = bitTest(singleWordInstruction, 5);
			dr = bitTest(singleWordInstruction, 8);
			
			numberRegister = (int) ((singleWordInstruction >> 9) & 0x7);
			
			if (size == 0b11) {		// memory shifting
				throw new RuntimeException("NOT !");
				
			} else {	//	register shifting
				data = D[register];
				
				if (!ir) {
					data = data >> numberRegister;
					D[register] = data;
					
				} else {
					throw new RuntimeException("NOT !");
				}
			}
			
			if (data == 0) {
				setZ();
			} else {
				clearZ();
			}
			if ((data & 0x8000_0000L) > 0) {	// TODO otros sizes ?
				setN();
			} else {
				clearN();
			}
			
			// TODO impl carry y X y Overflow
			
			break;
			
			default:
				throw new RuntimeException("Illegal opcode " + pad(opcode) + " found at pc: " + pad4((int) PC));
		}
		
		PC += 2;
		
		return cycles;
	}
	
	private long A(int register) {
		return A[register] & 0xFFFF_FFFFL;
	}

	public long copyMask(int registerListMaskD, int registerListMaskA, long desvio) {
		long data = 0;
		for (int i = 0; i < 8; i++) {
			if (((registerListMaskD) & (1 << i)) != 0) {
				data  = (bus.read(desvio)     << 24);
				data |= (bus.read(desvio + 1) << 16);
				data |= (bus.read(desvio + 2) << 8);
				data |= (bus.read(desvio + 3));
				
				D[i] = data;
				
				desvio += 4;
			}
		}
		for (int i = 0; i < 8; i++) {
			if (((registerListMaskA) & (1 << i)) != 0) {
				data  = (bus.read(desvio)     << 24);
				data |= (bus.read(desvio + 1) << 16);
				data |= (bus.read(desvio + 2) << 8);
				data |= (bus.read(desvio + 3));
				
				A[i] = data;
				
				desvio += 4;
			}
		}
		
		return desvio;
	}

//	If condition true then program execution continues at:
//		(PC) + offset.
//		PC value is instruction address more two.
//		Offset is the relative value in bytes which separate Bcc instruction
//		of mentioned label.
//	Condition code 'cc' specifies one of the following:
//		0000 F  False            Z = 1      1000 VC oVerflow Clear   V = 0
//		0001 T  True             Z = 0      1001 VS oVerflow Set     V = 1
//		0010 HI HIgh             C + Z = 0  1010 PL PLus             N = 0
//		0011 LS Low or Same      C + Z = 1  1011 MI MInus            N = 1
//		0100 CC Carry Clear      C = 0      1100 GE Greater or Equal N (+) V = 0
//		0101 CS Carry Set        C = 1      1101 LT Less Than        N (+) V = 1
//		0110 NE Not Equal        Z = 0      1110 GT Greater Than     Z + (N (+) V) = 0
//		0111 EQ EQual            Z = 1      1111 LE Less or Equal    Z + (N (+) V) = 1
	private void branch(boolean condition, int offset, int operand) {
		if (condition) {
			if (offset == 0) {	//	16 BITS OFFSET, IF 8 BITS OFFSET = $00
				if (operand > 0x7FFF) {
					operand = operand - 0xFFFF - 1;	// para que sea signed, TODO arreglar esto
				}
				
				PC += operand;
				
			} else if (offset == 0xFF) {	//	32 BITS OFFSET, IF 8 BITS OFFSET = $FF
				throw new RuntimeException("NOT IMPL BRANCH 32");
				
			} else {	//	8-Bit Displacement field
				if (offset > 0x7F) {
					offset = offset - 0xFF - 1;	// para que sea signed, TODO arreglar esto
				}
				
				PC += offset;
			}
		} else {
			if (offset == 0) {	//	16 BITS OFFSET, IF 8 BITS OFFSET = $00
				PC += 2;
			} else if (offset == 0xFF) {	//	32 BITS OFFSET, IF 8 BITS OFFSET = $FF
				PC += 4;
			} else {	//	8-Bit Displacement field
				// NADA
			}
		}
	}

	public void setX() {
		SR = bitSet(SR, 4);
	}
	
	public void clearX() {
		SR = bitReset(SR, 4);
	}
	
	public void setN() {
		SR = bitSet(SR, 3);
	}
	
	public void clearN() {
		SR = bitReset(SR, 3);
	}
	
	public void setZ() {
		SR = bitSet(SR, 2);
	}
	
	public void clearZ() {
		SR = bitReset(SR, 2);
	}
	
	public boolean isZ() {
		return bitTest(SR, 2);
	}
	
	public void setV() {
		SR = bitSet(SR, 1);
	}
	
	public void clearV() {
		SR = bitReset(SR, 1);
	}
	
	public void setC() {
		SR = bitSet(SR, 0);
	}
	
	public void clearC() {
		SR = bitReset(SR, 0);
	}
	
	public boolean isC() {
		return bitTest(SR, 0);
	}

	public final String pad(int reg) {
        String s = Integer.toHexString(reg).toUpperCase();
        if (s.length() == 1) {
            s = "0" + s;
        }
        return s;
    }

    public final String pad4(int reg) {
        String s = Integer.toHexString(reg).toUpperCase();
        while (s.length() < 4) {
            s = "0" + s;
        }
        return s;
    }

	public void initialize() {
		//	the processor fetches an initial stack pointer from locations $000000-$000003
		SSP |= (bus.read(0) << 24);
		SSP |= (bus.read(1) << 16);
		SSP |= (bus.read(2) << 8);
		SSP |= (bus.read(3) << 0);
		
		USP = 0xFFFFFFFF;

		//	initial PC specified by locations $000004-$000007
		PC |= (bus.read(4) << 24);
		PC |= (bus.read(5) << 16);
		PC |= (bus.read(6) << 8);
		PC |= (bus.read(7) << 0);
		
		for (int i = 0; i < A.length; i++) {
			A[i] = 0xFFFFFFFF;
			D[i] = 0xFFFFFFFF;
		}
		A[7] = SSP;
		
		SR = 0x7FFF;
	}
	
	public long readAddressingMode(Size size, int mode, int register) {
		return readAddressingMode(PC + 2, size, mode, register, true);
	}
	
	public long readAddressingMode(long offset, Size size, int mode, int register, boolean read) {
		long addr = 0;
		long data = 0;
		
		if (mode == 0b000) {	//	Dn
			if (size == Size.byt) {		//	byte
				data = D[register] & 0xFF;
			} else if (size == Size.word) {		//	word
				data = D[register] & 0xFFFF;
			} else if (size == Size.longW) {		//	long
				data = D[register];
			}
		} else if (mode == 0b001) {		//	An		Address Register Direct Mode 
			if (size == Size.byt) {		//	byte
				data = A[register] & 0xFF;
			} else if (size == Size.word) {		//	word
				data = A[register] & 0xFFFF;
			} else if (size == Size.longW) {		//	long
				data = A[register];
			}
			
		} else if (mode == 0b010) {		//	(An)	Address Register Indirect Mode
			addr = A[register];
			
			if (size == Size.byt) {	// size = Byte TODO checkear esto
				data = (bus.read(addr));
			
			} else if (size == Size.word) {		// size = word
				data  = (bus.read(addr) << 8);
				data |= (bus.read(addr + 1));
			
			
			} else if (size == Size.longW) {	//	size = Long
				data  = (bus.read(addr)     << 24);
				data |= (bus.read(addr + 1) << 16);
				data |= (bus.read(addr + 2) << 8);
				data |= (bus.read(addr + 3) << 0);
				
			}
		} else if (mode == 0b011) {		//	(An)+	 Address Register Indirect with Postincrement Mode 
			addr = A[register];
			
			if (size == Size.byt) {	//	byte
				data = bus.read(addr);
				A[register]++;
				
			} else if (size == Size.word) {	//	word
				data  = (bus.read(addr)     << 8);
				data |= (bus.read(addr + 1) << 0);
				A[register] += 2;
				
			} else if (size == Size.longW) {	//	long
				data  = (bus.read(addr)     << 24);
				data |= (bus.read(addr + 1) << 16);
				data |= (bus.read(addr + 2) << 8);
				data |= (bus.read(addr + 3) << 0);
				A[register] += 4;
				
			}
				
		} else if (mode == 0b100) {		//	-(An)	 Address Register Indirect with Predecrement Mode 
			addr = A[register];
			
			if (size == Size.byt) {	//	byte
				addr--;
				A[register] = addr;
				data = bus.read(addr);
				
			} else if (size == Size.word) {	//	word
				addr -= 2;
				A[register] = addr;
				data  = (bus.read(addr)     << 8);
				data |= (bus.read(addr + 1) << 0);
				
			} else if (size == Size.longW) {	//	long
				addr -= 4;
				A[register] = addr;
				data  = (bus.read(addr)     << 24);
				data |= (bus.read(addr + 1) << 16);
				data |= (bus.read(addr + 2) << 8);
				data |= (bus.read(addr + 3) << 0);
				
			}
			
		} else if (mode == 0b101) {	//	(d16,An)	Address with Displacement
			long base = A[register];
			long displac = bus.read(PC + 2) << 8;
			displac |= bus.read(PC + 3);
			
			long displacement = (long) displac;
			if ((displacement & 0x8000) > 0) {
				displacement |= 0xFFFF_0000L;	// sign extend 32 bits
			}
			addr = (base + displacement);
			data = bus.read(addr);
			
			PC += 2;
			
		} else if (mode == 0b111) {
			if (register == 0b000) {		//	Abs.W
				addr  = (bus.read(offset) << 8);
				addr |= (bus.read(offset + 1) << 0);
			
				if ((addr & 0x8000) > 0) {
					addr |= 0xFFFF_0000;
				}
				
				if (read) {
					if (size == Size.byt) {
						data = bus.read(addr);
					} else if (size == Size.word) {
						data = (bus.read(addr) << 8);
						data |= bus.read(addr + 1);
					} else {
						throw new RuntimeException("AA");
					}
				} else {
					data = addr;
				}
				
				PC += 2;
				
			} else if (register == 0b001) {		//	Abs.L
				addr  = (bus.read(offset) << 24);
				addr |= (bus.read(offset + 1) << 16);
				addr |= (bus.read(offset + 2) << 8);		
				addr |= (bus.read(offset + 3));
			
				if (read) {
					data = (bus.read(addr));		// FIXME porq aca es directo, y el de abajo es bus.read ?
				} else {
					data = addr;
				}
				
				PC += 4;
				
				cycles = 10;
				
			} else if (register == 0b010) {		//	 (d16,PC)	Program Counter Indirect with Displacement Mode
				long displacement = (bus.read(PC + 2) << 8);
				displacement 	 |= (bus.read(PC + 3));
				
				data = PC + 2 + displacement;
				
				PC += 2;
				// TODO check otros sizes
				
			} else if (register == 0b100) {		//	#data
				if (size == Size.word) {	//	word
					data  = (bus.read(PC + 2) << 8);
					data |= (bus.read(PC + 3) << 0);
					
					PC += 2;
					
				} else if (size == Size.longW) {	// long
					data  = (bus.read(PC + 2) << 24);
					data |= (bus.read(PC + 3) << 16);
					data |= (bus.read(PC + 4) << 8);
					data |= (bus.read(PC + 5));
					
					PC += 4;
					
				} else if (size == Size.byt) {		//	aunque sea byte, siempre ocupa 2 bytes y cuenta el de la derecha
					data  = (bus.read(PC + 2) << 8);
					data |= (bus.read(PC + 3) << 0);
					
					data = data & 0xFF;
					
					PC += 2;
					
				} else {
					throw new RuntimeException("NOT IMPLE");
				}
				
			} else {
				throw new RuntimeException("Addressing no soportado: " + mode + " REG: " + register);
			}
		} else {
			throw new RuntimeException("Addressing no soportado: " + mode);
		}
		
		return data;
	}
	
	public void writeAddressingMode(Size size, long offset, long data, int mode, int register) {
		long addr;
		
		if (mode == 0b000) {	//	Dn
			if (size == Size.byt) {		//	byte
				long old = D[register];
				long v = (old & (0xFFFF_FF00)) | data;
				D[register] = v;	// size = byte, se escribe el ultimo byte
				
			} else if (size == Size.word) {		//	word
				long old = D[register];
				long v = (old & (0xFFFF_0000)) | data;
				D[register] = v;	// size = word, se escribe los 2 ultimos bytes
				
			} else if (size == Size.longW) {		//	long
				D[register] = data;
			}
			
		} else if (mode == 0b001) {		//	An		Address Register Direct Mode 
			
			throw new RuntimeException("IMPL");
			
		} else if (mode == 0b010) {		//	(An)	Address Register Indirect Mode
			addr = A[register];
			
			if (size == Size.longW) {
				bus.write(addr, (data >> 16), size);
				bus.write(addr + 2, (data & 0xFFFF), size);
				
			} else if (size == Size.word) {
				bus.write(addr, (data & 0xFFFF), size);
				
			} else if (size == Size.byt) {
				bus.write(addr, (data & 0xFF), size);
			}
			
		} else if (mode == 0b011) {		//	(An)+	 Address Register Indirect with Postincrement Mode 
			addr = A[register];
			
			if (size == Size.byt) {		//	byte
				bus.write(addr, data & 0xFF, size);
				A[register]++;
			} else if (size == Size.word) {	//	word
				bus.write(addr, data & 0xFFFF, size);
				A[register] += 2;
			} else if (size == Size.longW) {	//	long
				bus.write(addr, data >> 16, size);
				bus.write(addr + 2, data & 0xFFFF, size);
				A[register] += 4;
			}
		} else if (mode == 0b100) {		//	-(An)
			long address = A[register];
			
			if (size == Size.longW) {	//	long word
				address = (address - 4) & 0xFFFFFFFF;
				A[register] = address;	// predecrement
				
				bus.write(address, (data >> 16) & 0xFFFF, size);
				bus.write(address + 2, (data & 0xFFFF), size);
				
			} else {
				throw new RuntimeException("NOT " + size);
			}
			
		} else if (mode == 0b101) {		//	(d16,An)	Address with Displacement
			if (size == Size.byt) {	//	byte
				long base = A[register];
				long displac = (bus.read(offset) << 8);
				displac 	|= (bus.read(offset + 1));
				
				if ((displac & 0x80) > 0) {
					displac |= 0xFFFF_FF00L;	// sign extend 32 bits
				}
				addr = (base + displac);
				bus.write(addr, data & 0xFF, size);
				
				PC += 2;
				
			} else if (size == Size.word) {	//	word
				throw new RuntimeException("NOO");
			} else if (size == Size.longW) {	//	long
				throw new RuntimeException("NOO");
			}
			
		} else if (mode == 0b111) {
			if (register == 0b000) {			//	Abs.W
				addr  = (bus.read(offset) << 8);
				addr |= (bus.read(offset + 1) << 0);
			
				if ((addr & 0x8000) > 0) {
					addr |= 0xFFFF_0000;
				}
				
				if (size == Size.byt) {
					bus.write(addr, data, size);
				} else {
					throw new RuntimeException("");
				}
				
				PC += 2;
				
			} else if (register == 0b001) {		//	Abs.L
				addr  = (bus.read(offset) << 24);
				addr |= (bus.read(offset + 1) << 16);
				addr |= (bus.read(offset + 2) << 8);
				addr |= (bus.read(offset + 3) << 0);
			
				if (size == Size.byt) {
					bus.write(addr, data & 0xFF, size);
					
				} else if (size == Size.word) {
					bus.write(addr, data & 0xFFFF, size);
					
				} else if (size == Size.longW) {
					bus.write(addr, data >> 16, size);
					bus.write(addr + 2, data & 0xFFFF, size);
					
				}
				
				PC += 4;
				
			} else if (register == 0b010) {		//	(d16,PC)	Program Counter Indirect with Displacement Mode
				throw new RuntimeException("IMPL");
			} else if (register == 0b100) {		//	#data
				throw new RuntimeException("IMPL");
			} else {
				throw new RuntimeException("Addressing no soportado: " + mode + " REG: " + register);
			}
		} else {
			throw new RuntimeException("Addressing no soportado: " + mode);
		}
	}
	
	private Size mapSize(int siz) {
		if (siz == 0b01) {
			return Size.byt;
		} else if (siz == 0b11) {
			return Size.word;
		} else if (siz == 0b10) {
			return Size.longW;
		}
		return null;
	}
	
//	SIZE
//	00->one Byte operation
//	01->one Word operation
//	10->one Long operation
	private Size mapAlternateSize(int size) {
		if (size == 0b00) {
			return Size.byt;
		} else if (size == 0b01) {
			return Size.word;
		} else if (size == 0b10) {
			return Size.longW;
		}
		return null;
	}
	
//	OP-MODE
//	Byte	Word	Long
//	~~~~	~~~~	~~~~
//	000		001		010	(Dn) - (<ea>) -> Dn
//	100		101		110	(<ea>) - (Dn) -> <ea>
	private Size mapFromOpMode(int size) {
		if (size == 0b000 || size == 0b100) {
			return Size.byt;
		} else if (size == 0b001 || size == 0b101) {
			return Size.word;
		} else if (size == 0b010 || size == 0b110) {
			return Size.longW;
		}
		return null;
	}

	public boolean bitTest(long address, int position) {
        return ((address & (1 << position)) != 0);
    }

    public int bitSet(int address, int position) {
        return address | (1 << position);
    }

    public int bitReset(int address, int position) {
        return address & ~(1 << position);
    }
    
    int getInterruptMask() {
    	return (SR >> 8) & 0x7;
    }

	public void addInstruction(int opcode, GenInstruction ins) {
		GenInstruction instr = instructions[opcode];
		if (instr != null) {
			throw new RuntimeException(pad4(opcode));
		}
		instructions[opcode] = ins;
	}
	
}
