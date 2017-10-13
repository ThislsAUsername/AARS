   package mars.mips.instructions;
   import mars.simulator.*;
   import mars.mips.hardware.*;
   import mars.mips.instructions.syscalls.*;
   import mars.*;
   import mars.util.*;
   import java.util.*;
   import java.io.*;
	
	/*
Copyright (c) 2003-2013,  Pete Sanderson and Kenneth Vollmar

Developed by Pete Sanderson (psanderson@otterbein.edu)
and Kenneth Vollmar (kenvollmar@missouristate.edu)

Permission is hereby granted, free of charge, to any person obtaining 
a copy of this software and associated documentation files (the 
"Software"), to deal in the Software without restriction, including 
without limitation the rights to use, copy, modify, merge, publish, 
distribute, sublicense, and/or sell copies of the Software, and to 
permit persons to whom the Software is furnished to do so, subject 
to the following conditions:

The above copyright notice and this permission notice shall be 
included in all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, 
EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF 
MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. 
IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR 
ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF 
CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION 
WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.

(MIT license, http://www.opensource.org/licenses/mit-license.html)
 */

/**
 * The list of Instruction objects, each of which represents a MIPS instruction.
 * The instruction may either be basic (translates into binary machine code) or
 * extended (translates into sequence of one or more basic instructions).
 *
 * @author Pete Sanderson and Ken Vollmar
 * @version August 2003-5
 */

    public class InstructionSet
   {
      private ArrayList<Instruction> instructionList;
	  private ArrayList<MatchMap> opcodeMatchMaps;
      private SyscallLoader syscallLoader;
      private static final String DOUBLE_SIZE_REGISTER_ERROR = "All floating-point registers must be even-numbered or use the D0,D1,etc syntax";
    /**
     * Creates a new InstructionSet object.
     */
       public InstructionSet()
      {
         instructionList = new ArrayList<Instruction>();
      
      }
    /**
     * Retrieve the current instruction set.
     */
       public ArrayList<Instruction> getInstructionList()
      {
         return instructionList;
      
      }
    /**
     * Adds all instructions to the set.  A given extended instruction may have
     * more than one Instruction object, depending on how many formats it can have.
     * @see Instruction
     * @see BasicInstruction
     * @see ExtendedInstruction
     */
       public void populate()
      {
        /* Here is where the parade begins.  Every instruction is added to the set here.*/
      
        // ////////////////////////////////////   BASIC INSTRUCTIONS START HERE ////////////////////////////////
      
         instructionList.add(
                new BasicInstruction("nop",
            	 "Null operation : machine code is all zeroes",
                BasicInstructionFormat.R_FORMAT,
                "000000 00000 00000 00000 00000 000000",
                new SimulationCode()
               {
                   public void simulate(ProgramStatement statement) throws ProcessingException
                  {
                  	// Hey I like this so far!
                  }
               }));
         instructionList.add(
                new BasicInstruction("ADD X1,X2,X3",
            	 "Addition with overflow : set X1 to (X2 plus X3)",
                BasicInstructionFormat.R_FORMAT,
                "000000 sssss ttttt fffff 00000 100000",
                new SimulationCode()
               {
                   public void simulate(ProgramStatement statement) throws ProcessingException
                  {
                     int[] operands = statement.getOperands();
                     int add1 = RegisterFile.getValue(operands[1]);
                     int add2 = RegisterFile.getValue(operands[2]);
                     int sum = add1 + add2;
                  // overflow on A+B detected when A and B have same sign and A+B has other sign.
                     if ((add1 >= 0 && add2 >= 0 && sum < 0)
                        || (add1 < 0 && add2 < 0 && sum >= 0))
                     {
                        throw new ProcessingException(statement,
                            "arithmetic overflow",Exceptions.ARITHMETIC_OVERFLOW_EXCEPTION);
                     }
                     RegisterFile.updateRegister(operands[0], sum);
                  }
               }));
         instructionList.add(
                 new BasicInstruction("ADDS X1,X2,X3",
             	 "Add and set flags : set X1 to (X2 plus X3), and set the processor flags",
                 BasicInstructionFormat.R_FORMAT,
                 "000000 sssss ttttt fffff 00000 100000",
                 new SimulationCode()
                {
                    public void simulate(ProgramStatement statement) throws ProcessingException
                   {
                      int[] operands = statement.getOperands();
                      long add1 = RegisterFile.getValue(operands[1]);
                      long add2 = RegisterFile.getValue(operands[2]);
                      long sum = add1 + add2;
                      // Stolen from https://static.docs.arm.com/ddi0553/a/DDI0553A_c_armv8m_arm.pdf#E26.AddWithCarry
                      // Except it's probably not stealing if you're simply implementing a specification?
                      // For carry, we have to simulate the behavior of the processor itself
                      // The two's complement math will put out a carry in the case of subtraction
                      // 	because the physical register isn't infinite length.
                      add1 &= 0xFFFFFFFFL;
                      add2 &= 0xFFFFFFFFL;
                      boolean carry = 0 < (add1 + add2)>>32;
                      // binary debug
                      //System.out.println(""+Binary.longToBinaryString(add1)+" +\n"+Binary.longToBinaryString(add2)+" =\n"+Binary.longToBinaryString(add1 + add2)+" >>32= "+((add1 + add2)>>32));
                      // For overflow, we truncate to int, then sign-extend it back to long and see if it changes.
                      boolean overflow = (long) (int) sum != sum;
                      RegisterFile.setFlags((int) sum, overflow, carry);
                      RegisterFile.updateRegister(operands[0], (int) sum);
                   }
                }));
         instructionList.add(
                new BasicInstruction("SUB X1,X2,X3",
            	 "Subtraction with overflow : set X1 to (X2 minus X3)",
                BasicInstructionFormat.R_FORMAT,
                "000000 sssss ttttt fffff 00000 100010",
                new SimulationCode()
               {
                   public void simulate(ProgramStatement statement) throws ProcessingException
                  {
                     int[] operands = statement.getOperands();
                     int sub1 = RegisterFile.getValue(operands[1]);
                     int sub2 = RegisterFile.getValue(operands[2]);
                     int dif = sub1 - sub2;
                  // overflow on A-B detected when A and B have opposite signs and A-B has B's sign
                     if ((sub1 >= 0 && sub2 < 0 && dif < 0)
                        || (sub1 < 0 && sub2 >= 0 && dif >= 0))
                     {
                        throw new ProcessingException(statement,
                            "arithmetic overflow",Exceptions.ARITHMETIC_OVERFLOW_EXCEPTION);
                     }
                     RegisterFile.updateRegister(operands[0], dif);
                  }
               }));
         instructionList.add(
                 new BasicInstruction("SUBS X1,X2,X3",
             	 "Subtract and set flags : set X1 to (X2 minus X3), and set the processor flags",
                 BasicInstructionFormat.R_FORMAT,
                 "000000 sssss ttttt fffff 00000 100000",
                 new SimulationCode()
                {
                    public void simulate(ProgramStatement statement) throws ProcessingException
                   {
                      int[] operands = statement.getOperands();
                      long add1 = RegisterFile.getValue(operands[1]);
                      long add2 = -1*RegisterFile.getValue(operands[2]); // the only difference with ADDS is here
                      long sum = add1 + add2;
                      // Stolen from https://static.docs.arm.com/ddi0553/a/DDI0553A_c_armv8m_arm.pdf#E26.AddWithCarry
                      // Except it's probably not stealing if you're simply implementing a specification?
                      // For carry, we have to simulate the behavior of the processor itself
                      // The two's complement math will put out a carry in the case of subtraction
                      // 	because the physical register isn't infinite length.
                      add1 &= 0xFFFFFFFFL;
                      add2 &= 0xFFFFFFFFL;
                      boolean carry = 0 < (add1 + add2)>>32;
                      // binary debug
                      //System.out.println(""+Binary.longToBinaryString(add1)+" +\n"+Binary.longToBinaryString(add2)+" =\n"+Binary.longToBinaryString(add1 + add2)+" >>32= "+((add1 + add2)>>32));
                      // For overflow, we truncate to int, then sign-extend it back to long and see if it changes.
                      boolean overflow = (long) (int) sum != sum;
                      RegisterFile.setFlags((int) sum, overflow, carry);
                      RegisterFile.updateRegister(operands[0], (int) sum);
                   }
                }));
         instructionList.add(
                new BasicInstruction("ADDI X1,X2,100",
            	 "Addition immediate with overflow : set X1 to (X2 plus unsigned 16-bit immediate)",
                BasicInstructionFormat.I_FORMAT,
                "001000 sssss fffff tttttttttttttttt",
                new SimulationCode()
               {
                   public void simulate(ProgramStatement statement) throws ProcessingException
                  {
                     int[] operands = statement.getOperands();
                     int add1 = RegisterFile.getValue(operands[1]);
                     int add2 = operands[2] << 16 >> 16;
                     int sum = add1 + add2;
                  // overflow on A+B detected when A and B have same sign and A+B has other sign.
                     // no need to check add2 since we know the sign already
                     if (add1 >= 0 && sum < 0)
                     {
                        throw new ProcessingException(statement,
                            "arithmetic overflow",Exceptions.ARITHMETIC_OVERFLOW_EXCEPTION);
                     }
                     RegisterFile.updateRegister(operands[0], sum);
                  }
               }));
         instructionList.add(
                 new BasicInstruction("ADDIS X1,X2,100",
             	 "Add an immediate and set flags : set X1 to (X2 plus unsigned 16-bit immediate), and set the processor flags",
                 BasicInstructionFormat.R_FORMAT,
                 "000000 sssss ttttt fffff 00000 100000",
                 new SimulationCode()
                {
                    public void simulate(ProgramStatement statement) throws ProcessingException
                   {
                      int[] operands = statement.getOperands();
                      long add1 = RegisterFile.getValue(operands[1]);
                      long add2 = operands[2] << 16 >> 16; // only difference with ADDS
                      long sum = add1 + add2;
                      // Stolen from https://static.docs.arm.com/ddi0553/a/DDI0553A_c_armv8m_arm.pdf#E26.AddWithCarry
                      // Except it's probably not stealing if you're simply implementing a specification?
                      // For carry, we have to simulate the behavior of the processor itself
                      // The two's complement math will put out a carry in the case of subtraction
                      // 	because the physical register isn't infinite length.
                      add1 &= 0xFFFFFFFFL;
                      add2 &= 0xFFFFFFFFL;
                      boolean carry = 0 < (add1 + add2)>>32;
                      // binary debug
                      //System.out.println(""+Binary.longToBinaryString(add1)+" +\n"+Binary.longToBinaryString(add2)+" =\n"+Binary.longToBinaryString(add1 + add2)+" >>32= "+((add1 + add2)>>32));
                      // For overflow, we truncate to int, then sign-extend it back to long and see if it changes.
                      boolean overflow = (long) (int) sum != sum;
                      RegisterFile.setFlags((int) sum, overflow, carry);
                      RegisterFile.updateRegister(operands[0], (int) sum);
                   }
                }));
         instructionList.add(
                 new BasicInstruction("SUBI X1,X2,100",
             	 "Subtraction immediate with overflow : set X1 to (X2 minus unsigned 16-bit immediate)",
                 BasicInstructionFormat.I_FORMAT,
                 "001000 sssss fffff tttttttttttttttt",
                 new SimulationCode()
                {
                    public void simulate(ProgramStatement statement) throws ProcessingException
                   {
                      int[] operands = statement.getOperands();
                      int add1 = RegisterFile.getValue(operands[1]);
                      int add2 = operands[2] << 16 >> 16;
                      int sum = add1 - add2;
                   // overflow on A+B detected when A and B have same sign and A+B has other sign.
                      // no need to check add2 since we know the sign already
                      if (add1 < 0 && sum >= 0)
                      {
                         throw new ProcessingException(statement,
                             "arithmetic overflow",Exceptions.ARITHMETIC_OVERFLOW_EXCEPTION);
                      }
                      RegisterFile.updateRegister(operands[0], sum);
                   }
                }));
         instructionList.add(
                 new BasicInstruction("SUBIS X1,X2,100",
             	 "Subtract an immediate and set flags : set X1 to (X2 minus unsigned 16-bit immediate), and set the processor flags",
                 BasicInstructionFormat.R_FORMAT,
                 "000000 sssss ttttt fffff 00000 100000",
                 new SimulationCode()
                {
                    public void simulate(ProgramStatement statement) throws ProcessingException
                   {
                      int[] operands = statement.getOperands();
                      long add1 = RegisterFile.getValue(operands[1]);
                      long add2 = -1*operands[2] << 16 >> 16; // only difference with ADDIS
                      long sum = add1 + add2;
                      // Stolen from https://static.docs.arm.com/ddi0553/a/DDI0553A_c_armv8m_arm.pdf#E26.AddWithCarry
                      // Except it's probably not stealing if you're simply implementing a specification?
                      // For carry, we have to simulate the behavior of the processor itself
                      // The two's complement math will put out a carry in the case of subtraction
                      // 	because the physical register isn't infinite length.
                      add1 &= 0xFFFFFFFFL;
                      add2 &= 0xFFFFFFFFL;
                      boolean carry = 0 < (add1 + add2)>>32;
                      // binary debug
                      //System.out.println(""+Binary.longToBinaryString(add1)+" +\n"+Binary.longToBinaryString(add2)+" =\n"+Binary.longToBinaryString(add1 + add2)+" >>32= "+((add1 + add2)>>32));
                      // For overflow, we truncate to int, then sign-extend it back to long and see if it changes.
                      boolean overflow = (long) (int) sum != sum;
                      RegisterFile.setFlags((int) sum, overflow, carry);
                      RegisterFile.updateRegister(operands[0], (int) sum);
                   }
                }));
         instructionList.add(
                new BasicInstruction("MUL X1,X2,X3",
            	 "Multiplication without overflow: Set X1 to low-order 32 bits of the product of X2 and X3",
                BasicInstructionFormat.R_FORMAT,
                "011100 sssss ttttt fffff 00000 000010",
                new SimulationCode()
               {
                   public void simulate(ProgramStatement statement) throws ProcessingException
                  {
                     int[] operands = statement.getOperands();
                     long product = (long) RegisterFile.getValue(operands[1])
                        * (long) RegisterFile.getValue(operands[2]);
                     RegisterFile.updateRegister(operands[0], (int) ((product << 32) >> 32));                  }
               }));
         instructionList.add(
                 new BasicInstruction("SMULH X1,X2,X3",
             	 "Signed Multiplication High: Set X1 to high-order 32 bits of the product of X2 and X3",
                 BasicInstructionFormat.R_FORMAT,
                 "011100 sssss ttttt fffff 00000 000010",
                 new SimulationCode()
                {
                    public void simulate(ProgramStatement statement) throws ProcessingException
                   {
                      int[] operands = statement.getOperands();
                      long product = (long) RegisterFile.getValue(operands[1])
                         * (long) RegisterFile.getValue(operands[2]);
                      RegisterFile.updateRegister(operands[0], (int) (product >> 32));
                   }
                }));
         instructionList.add(
                 new BasicInstruction("UMULH X1,X2,X3",
             	 "Unsigned Multiplication High: Set X1 to high-order 32 bits of the product of unsigned X1 and X2",
                 BasicInstructionFormat.R_FORMAT,
                 "011100 sssss ttttt fffff 00000 000010",
                 new SimulationCode()
                {
                    public void simulate(ProgramStatement statement) throws ProcessingException
                   {
                      int[] operands = statement.getOperands();
                      long product = (((long) RegisterFile.getValue(operands[1]))<<32>>>32)
                         * (((long) RegisterFile.getValue(operands[2]))<<32>>>32);
                      RegisterFile.updateRegister(operands[0], (int) (product >> 32));
                   }
                }));
         instructionList.add(
                new BasicInstruction("SDIV X1,X2,X3",
            	 "Division: Divide X2 by X3 then set X1 to quotient",
                BasicInstructionFormat.R_FORMAT,
                "011100 sssss ttttt fffff 00000 000010",
                new SimulationCode()
               {
                   public void simulate(ProgramStatement statement) throws ProcessingException
                  {
                     int[] operands = statement.getOperands();
                     if (RegisterFile.getValue(operands[2]) == 0)
                     {
                     // Note: no exceptions and undefined results for zero div
                     // COD3 Appendix A says "with overflow" but MIPS 32 instruction set
                     // specification says "no arithmetic exception under any circumstances".
                        return;
                     }
                  
                     RegisterFile.updateRegister(operands[0],
                        RegisterFile.getValue(operands[1])
                        / RegisterFile.getValue(operands[2]));
                  }
               }));
         instructionList.add(
                new BasicInstruction("UDIV X1,X2,X3",
            	 "Division unsigned: Divide unsigned X2 by X3 then set X1 to quotient",
                BasicInstructionFormat.R_FORMAT,
                "011100 sssss ttttt fffff 00000 000010",
                new SimulationCode()
               {
                   public void simulate(ProgramStatement statement) throws ProcessingException
                  {
                     int[] operands = statement.getOperands();
                     if (RegisterFile.getValue(operands[2]) == 0)
                     {
                     // Note: no exceptions, and undefined results for zero divide
                        return;
                     }
                     long oper1 = ((long)RegisterFile.getValue(operands[1])) << 32 >>> 32; 
                     long oper2 = ((long)RegisterFile.getValue(operands[2])) << 32 >>> 32; 
                     RegisterFile.updateRegister(operands[0],
                        (int) (((oper1 / oper2) << 32) >> 32));                  
                  }
               }));
         instructionList.add(
                new BasicInstruction("AND X1,X2,X3",
            	 "Bitwise AND : Set X1 to bitwise AND of X2 and X3",
                BasicInstructionFormat.R_FORMAT,
                "000000 sssss ttttt fffff 00000 100100",
                new SimulationCode()
               {
                   public void simulate(ProgramStatement statement) throws ProcessingException
                  {
                     int[] operands = statement.getOperands();
                     RegisterFile.updateRegister(operands[0],
                        RegisterFile.getValue(operands[1])
                        & RegisterFile.getValue(operands[2]));
                  }
               }));
         instructionList.add(
                 new BasicInstruction("ANDS X1,X2,X3",
             	 "Bitwise AND and set flags: Set X1 to bitwise AND of X2 and X3, then set Negative and Zero without touching Carry or oVerflow",
                 BasicInstructionFormat.R_FORMAT,
                 "000000 sssss ttttt fffff 00000 100100",
                 new SimulationCode()
                {
                    public void simulate(ProgramStatement statement) throws ProcessingException
                   {
                      int[] operands = statement.getOperands();
                      int andResult = RegisterFile.getValue(operands[1]) & RegisterFile.getValue(operands[2]);
                      RegisterFile.updateRegister(operands[0],andResult);
                      // From what I can tell, the ARM ARM indicates that oVerflow is always unaffected
                      // 	and Carry is unaffected if there is no shift.
                      RegisterFile.setFlags(andResult, RegisterFile.flagV(), RegisterFile.flagC());
                   }
                }));
         instructionList.add(
                 new BasicInstruction("ANDI X1,X2,100",
             	 "Bitwise AND immediate : Set X1 to bitwise AND of X2 and zero-extended 16-bit immediate",
                 BasicInstructionFormat.I_FORMAT,
                 "001100 sssss fffff tttttttttttttttt",
                 new SimulationCode()
                {
                    public void simulate(ProgramStatement statement) throws ProcessingException
                   {
                      int[] operands = statement.getOperands();
                   // ANDing with 0x0000FFFF zero-extends the immediate (high 16 bits always 0).
                      RegisterFile.updateRegister(operands[0],
                         RegisterFile.getValue(operands[1])
                         & (operands[2] & 0x0000FFFF));
                   }
                }));
         instructionList.add(
                 new BasicInstruction("ANDIS X1,X2,100",
             	 "Bitwise AND immediate and set flags: Set X1 to bitwise AND of X2 and zero-extended 16-bit immediate, then set Negative and Zero without touching Carry or oVerflow",
                 BasicInstructionFormat.I_FORMAT,
                 "001100 sssss fffff tttttttttttttttt",
                 new SimulationCode()
                {
                    public void simulate(ProgramStatement statement) throws ProcessingException
                   {
                      int[] operands = statement.getOperands();
                   // ANDing with 0x0000FFFF zero-extends the immediate (high 16 bits always 0).
                      int andResult = RegisterFile.getValue(operands[1]) & (operands[2] & 0x0000FFFF);
                      RegisterFile.updateRegister(operands[0],andResult);
                      // From what I can tell, the ARM ARM indicates that oVerflow is always unaffected
                      // 	and Carry is unaffected if there is no shift.
                      RegisterFile.setFlags(andResult, RegisterFile.flagV(), RegisterFile.flagC());
                   }
                }));
         instructionList.add(
                new BasicInstruction("ORR X1,X2,X3",
            	 "Bitwise OR : Set X1 to bitwise OR of X2 and X3",
                BasicInstructionFormat.R_FORMAT,
                "000000 sssss ttttt fffff 00000 100101",
                new SimulationCode()
               {
                   public void simulate(ProgramStatement statement) throws ProcessingException
                  {
                     int[] operands = statement.getOperands();
                     RegisterFile.updateRegister(operands[0],
                        RegisterFile.getValue(operands[1])
                        | RegisterFile.getValue(operands[2]));
                  }
               }));
         instructionList.add(
                new BasicInstruction("ORRI X1,X2,100",
            	 "Bitwise OR immediate : Set X1 to bitwise OR of X2 and zero-extended 16-bit immediate",
                BasicInstructionFormat.I_FORMAT,
                "001101 sssss fffff tttttttttttttttt",
                new SimulationCode()
               {
                   public void simulate(ProgramStatement statement) throws ProcessingException
                  {
                     int[] operands = statement.getOperands();
                  // ANDing with 0x0000FFFF zero-extends the immediate (high 16 bits always 0).
                     RegisterFile.updateRegister(operands[0],
                        RegisterFile.getValue(operands[1])
                        | (operands[2] & 0x0000FFFF));
                  }
               }));
         instructionList.add(
                new BasicInstruction("EOR X1,X2,X3",
            	 "Bitwise XOR (exclusive OR) : Set X1 to bitwise XOR of X2 and X3",
                BasicInstructionFormat.R_FORMAT,
                "000000 sssss ttttt fffff 00000 100110",
                new SimulationCode()
               {
                   public void simulate(ProgramStatement statement) throws ProcessingException
                  {
                     int[] operands = statement.getOperands();
                     RegisterFile.updateRegister(operands[0],
                        RegisterFile.getValue(operands[1])
                        ^ RegisterFile.getValue(operands[2]));
                  }
               }));
         instructionList.add(
                new BasicInstruction("EORI X1,X2,100",
            	 "Bitwise XOR immediate : Set X1 to bitwise XOR of X2 and zero-extended 16-bit immediate",
                BasicInstructionFormat.I_FORMAT,
                "001110 sssss fffff tttttttttttttttt",
                new SimulationCode()
               {
                   public void simulate(ProgramStatement statement) throws ProcessingException
                  {
                     int[] operands = statement.getOperands();
                  // ANDing with 0x0000FFFF zero-extends the immediate (high 16 bits always 0).
                     RegisterFile.updateRegister(operands[0],
                        RegisterFile.getValue(operands[1])
                        ^ (operands[2] & 0x0000FFFF));
                  }
               }));					
         instructionList.add(
                new BasicInstruction("LSL X1,X2,10",
            	 "Logical shift left: Set X1 to result of shifting X2 left by number of bits specified by immediate",
                BasicInstructionFormat.R_FORMAT,
                "000000 00000 sssss fffff ttttt 000000",
                new SimulationCode()
               {
                   public void simulate(ProgramStatement statement) throws ProcessingException
                  {
                     int[] operands = statement.getOperands();
                     RegisterFile.updateRegister(operands[0],
                        RegisterFile.getValue(operands[1]) << operands[2]);
                  }
               }));
         instructionList.add(
                new BasicInstruction("LSR X1,X2,10",
            	 "Shift right : Set X1 to result of shifting X2 right by number of bits specified by immediate",
                BasicInstructionFormat.R_FORMAT,
                "000000 00000 sssss fffff ttttt 000010",
                new SimulationCode()
               {
                   public void simulate(ProgramStatement statement) throws ProcessingException
                  {
                     int[] operands = statement.getOperands();
                  // must zero-fill, so use ">>>" instead of ">>".
                     RegisterFile.updateRegister(operands[0],
                        RegisterFile.getValue(operands[1]) >>> operands[2]);
                  }
               }));
         instructionList.add(
                 new BasicInstruction("B target", 
             	 "Branch unconditionally : Jump to statement at target address",
             	 BasicInstructionFormat.J_FORMAT,
                 "000010 ffffffffffffffffffffffffff",
                 new SimulationCode()
                {
                    public void simulate(ProgramStatement statement) throws ProcessingException
                   {
                      int[] operands = statement.getOperands();
                      processJump(
                         ((RegisterFile.getProgramCounter() & 0xF0000000)
                                 | (operands[0] << 2)));            
                   }
                }));
         instructionList.add(
                 new BasicInstruction("B -100", 
             	 "Branch unconditionally : Jump a number of instructions forward equal to the specified immediate",
             	 BasicInstructionFormat.R_FORMAT,
                 "000010 ffffffffffffffffffffffffff",
                 new SimulationCode()
                {
                    public void simulate(ProgramStatement statement) throws ProcessingException
                   {
                      int[] operands = statement.getOperands();
                      processJump(RegisterFile.getProgramCounter() + (operands[0]*4)-4);            
                   }
                }));
         instructionList.add(
                 new BasicInstruction("B.AL target", 
             	 "Branch always : Jump to statement at target address",
             	 BasicInstructionFormat.J_FORMAT,
                 "000010 ffffffffffffffffffffffffff",
                 new SimulationCode()
                {
                    public void simulate(ProgramStatement statement) throws ProcessingException
                   {
                      int[] operands = statement.getOperands();
                      processJump(
                         ((RegisterFile.getProgramCounter() & 0xF0000000)
                                 | (operands[0] << 2)));            
                   }
                }));
         instructionList.add(
                 new BasicInstruction("B.AL -100", 
             	 "Branch unconditionally : Jump a number of instructions forward equal to the specified immediate",
             	 BasicInstructionFormat.R_FORMAT,
                 "000010 ffffffffffffffffffffffffff",
                 new SimulationCode()
                {
                    public void simulate(ProgramStatement statement) throws ProcessingException
                   {
                      int[] operands = statement.getOperands();
                      processJump(RegisterFile.getProgramCounter() + (operands[0]*4)-4);            
                   }
                }));
          instructionList.add(
                 new BasicInstruction("BL target",
                 "Branch with link : Set X30 to Program Counter (return address) then jump to statement at target address",
             	 BasicInstructionFormat.J_FORMAT,
                 "000011 ffffffffffffffffffffffffff",
                 new SimulationCode()
                {
                    public void simulate(ProgramStatement statement) throws ProcessingException
                   {
                      int[] operands = statement.getOperands();
                      //TODO: maybe make this not a constant?
                      processReturnAddress(30);// RegisterFile.updateRegister(31, RegisterFile.getProgramCounter());
                      processJump(
                         (RegisterFile.getProgramCounter() & 0xF0000000)
                                 | (operands[0] << 2));
                   }
                }));
          instructionList.add(
                 new BasicInstruction("BR X1", 
             	 "Branch register unconditionally : Jump to statement whose address is in X1",
             	 BasicInstructionFormat.R_FORMAT,
                 "000000 fffff 00000 00000 00000 001000",
                 new SimulationCode()
                {
                    public void simulate(ProgramStatement statement) throws ProcessingException
                   {
                      int[] operands = statement.getOperands();
                      processJump(RegisterFile.getValue(operands[0]));
                   }
                }));
          instructionList.add(
                  new BasicInstruction("CBNZ X1,label",
                  "Conditional branch not zero : Branch to statement at label's address if X1 is NOT zero",
              	 BasicInstructionFormat.I_BRANCH_FORMAT,
                  "000001 fffff 00000 ssssssssssssssss",
                  new SimulationCode()
                 {
                     public void simulate(ProgramStatement statement) throws ProcessingException
                    {
                       int[] operands = statement.getOperands();
                       if (RegisterFile.getValue(operands[0]) != 0)
                       {
                          processBranch(operands[1]);
                       }
                    }
                 }));
          instructionList.add(
                  new BasicInstruction("CBNZ X1,-100", 
              	 "Conditional branch not zero : If X1 is NOT zero, jump a number of instructions forward equal to the specified immediate",
              	 BasicInstructionFormat.I_BRANCH_FORMAT,
                 "000001 fffff 00000 ssssssssssssssss",
                  new SimulationCode()
                 {
                     public void simulate(ProgramStatement statement) throws ProcessingException
                    {
                       int[] operands = statement.getOperands();
                       if (RegisterFile.getValue(operands[0]) != 0)
                       {
                    	   processJump(RegisterFile.getProgramCounter() + (operands[1]*4)-4);
                       }        
                    }
                 }));
          instructionList.add(
                  new BasicInstruction("CBZ X1,label",
                  "Conditional branch zero : Branch to statement at label's address if X1 is zero",
              	 BasicInstructionFormat.I_BRANCH_FORMAT,
                  "000001 fffff 00000 ssssssssssssssss",
                  new SimulationCode()
                 {
                     public void simulate(ProgramStatement statement) throws ProcessingException
                    {
                       int[] operands = statement.getOperands();
                       if (RegisterFile.getValue(operands[0]) == 0)
                       {
                          processBranch(operands[1]);
                       }
                    }
                 }));
          instructionList.add(
                  new BasicInstruction("CBZ X1,-100", 
              	 "Conditional branch zero : If X1 is zero, jump a number of instructions forward equal to the specified immediate",
              	 BasicInstructionFormat.I_BRANCH_FORMAT,
                 "000001 fffff 00000 ssssssssssssssss",
                  new SimulationCode()
                 {
                     public void simulate(ProgramStatement statement) throws ProcessingException
                    {
                       int[] operands = statement.getOperands();
                       if (RegisterFile.getValue(operands[0]) == 0)
                       {
                    	   processJump(RegisterFile.getProgramCounter() + (operands[1]*4)-4);
                       }        
                    }
                 }));
          instructionList.add(
                  new BasicInstruction("B.EQ target", 
              	 "Branch on Equal: Jump to statement at target address if Zero flag is true; flags=x1xx",
              	 BasicInstructionFormat.J_FORMAT,
                  "000010 ffffffffffffffffffffffffff",
                  new SimulationCode()
                 {
                     public void simulate(ProgramStatement statement) throws ProcessingException
                    {
                     	if (RegisterFile.flagZ())
                     	{
 				            int[] operands = statement.getOperands();
 				            processJump(
 				               ((RegisterFile.getProgramCounter() & 0xF0000000)
 				                       | (operands[0] << 2)));
                     	}
                    }
                 }));
          instructionList.add(
                  new BasicInstruction("B.NE target", 
              	 "Branch on NOT equal: Jump to statement at target address if Zero flag is false; flags=x0xx",
              	 BasicInstructionFormat.J_FORMAT,
                  "000010 ffffffffffffffffffffffffff",
                  new SimulationCode()
                 {
                     public void simulate(ProgramStatement statement) throws ProcessingException
                    {
                     	if (!RegisterFile.flagZ())
                     	{
 				            int[] operands = statement.getOperands();
 				            processJump(
 				               ((RegisterFile.getProgramCounter() & 0xF0000000)
 				                       | (operands[0] << 2)));
                     	}
                    }
                 }));
          instructionList.add(
                  new BasicInstruction("B.CS target", 
              	 "Branch on Carry Set: Jump to statement at target address if Carry flag is true; flags=xxx1",
              	 BasicInstructionFormat.J_FORMAT,
                  "000010 ffffffffffffffffffffffffff",
                  new SimulationCode()
                 {
                     public void simulate(ProgramStatement statement) throws ProcessingException
                    {
                     	if (RegisterFile.flagC())
                     	{
 				            int[] operands = statement.getOperands();
 				            processJump(
 				               ((RegisterFile.getProgramCounter() & 0xF0000000)
 				                       | (operands[0] << 2)));
                     	}
                    }
                 }));
          instructionList.add(
                  new BasicInstruction("B.HS target", 
              	 "Branch on Unsigned Higher/Same: Jump to statement at target address if Carry flag is true; flags=xxx1",
              	 BasicInstructionFormat.J_FORMAT,
                  "000010 ffffffffffffffffffffffffff",
                  new SimulationCode()
                 {
                     public void simulate(ProgramStatement statement) throws ProcessingException
                    {
                     	if (RegisterFile.flagC())
                     	{
 				            int[] operands = statement.getOperands();
 				            processJump(
 				               ((RegisterFile.getProgramCounter() & 0xF0000000)
 				                       | (operands[0] << 2)));
                     	}
                    }
                 }));
          instructionList.add(
                  new BasicInstruction("B.CC target", 
              	 "Branch on Carry Clear: Jump to statement at target address if Carry flag is false; flags=xxx0",
              	 BasicInstructionFormat.J_FORMAT,
                  "000010 ffffffffffffffffffffffffff",
                  new SimulationCode()
                 {
                     public void simulate(ProgramStatement statement) throws ProcessingException
                    {
                     	if (!RegisterFile.flagC())
                     	{
 				            int[] operands = statement.getOperands();
 				            processJump(
 				               ((RegisterFile.getProgramCounter() & 0xF0000000)
 				                       | (operands[0] << 2)));
                     	}
                    }
                 }));
          instructionList.add(
                  new BasicInstruction("B.LO target", 
              	 "Branch on Unsigned Lower: Jump to statement at target address if Carry flag is false; flags=xxx0",
              	 BasicInstructionFormat.J_FORMAT,
                  "000010 ffffffffffffffffffffffffff",
                  new SimulationCode()
                 {
                     public void simulate(ProgramStatement statement) throws ProcessingException
                    {
                     	if (!RegisterFile.flagC())
                     	{
 				            int[] operands = statement.getOperands();
 				            processJump(
 				               ((RegisterFile.getProgramCounter() & 0xF0000000)
 				                       | (operands[0] << 2)));
                     	}
                    }
                 }));
          instructionList.add(
                  new BasicInstruction("B.MI target", 
              	 "Branch on Minus: Jump to statement at target address if Negative flag is true; flags=1xxx",
              	 BasicInstructionFormat.J_FORMAT,
                  "000010 ffffffffffffffffffffffffff",
                  new SimulationCode()
                 {
                     public void simulate(ProgramStatement statement) throws ProcessingException
                    {
                     	if (RegisterFile.flagN())
                     	{
 				            int[] operands = statement.getOperands();
 				            processJump(
 				               ((RegisterFile.getProgramCounter() & 0xF0000000)
 				                       | (operands[0] << 2)));
                     	}
                    }
                 }));
          instructionList.add(
                  new BasicInstruction("B.PL target", 
              	 "Branch on Plus: Jump to statement at target address if Negative flag is false; flags=0xxx",
              	 BasicInstructionFormat.J_FORMAT,
                  "000010 ffffffffffffffffffffffffff",
                  new SimulationCode()
                 {
                     public void simulate(ProgramStatement statement) throws ProcessingException
                    {
                     	if (!RegisterFile.flagN())
                     	{
 				            int[] operands = statement.getOperands();
 				            processJump(
 				               ((RegisterFile.getProgramCounter() & 0xF0000000)
 				                       | (operands[0] << 2)));
                     	}
                    }
                 }));
          instructionList.add(
                  new BasicInstruction("B.VS target", 
              	 "Branch on Overflow: Jump to statement at target address if oVerflow flag is true; flags=xx1x",
              	 BasicInstructionFormat.J_FORMAT,
                  "000010 ffffffffffffffffffffffffff",
                  new SimulationCode()
                 {
                     public void simulate(ProgramStatement statement) throws ProcessingException
                    {
                     	if (RegisterFile.flagV())
                     	{
 				            int[] operands = statement.getOperands();
 				            processJump(
 				               ((RegisterFile.getProgramCounter() & 0xF0000000)
 				                       | (operands[0] << 2)));
                     	}
                    }
                 }));
          instructionList.add(
                  new BasicInstruction("B.VC target", 
              	 "Branch on No overflow: Jump to statement at target address if oVerflow flag is false; flags=xx0x",
              	 BasicInstructionFormat.J_FORMAT,
                  "000010 ffffffffffffffffffffffffff",
                  new SimulationCode()
                 {
                     public void simulate(ProgramStatement statement) throws ProcessingException
                    {
                     	if (!RegisterFile.flagV())
                     	{
 				            int[] operands = statement.getOperands();
 				            processJump(
 				               ((RegisterFile.getProgramCounter() & 0xF0000000)
 				                       | (operands[0] << 2)));
                     	}
                    }
                 }));
          instructionList.add(
                  new BasicInstruction("B.HI target", 
              	 "Branch on Unsigned Higher: Jump to statement at target address if Carry is true and Zero is false; flags=x0x1",
              	 BasicInstructionFormat.J_FORMAT,
                  "000010 ffffffffffffffffffffffffff",
                  new SimulationCode()
                 {
                     public void simulate(ProgramStatement statement) throws ProcessingException
                    {
                     	if (RegisterFile.flagC() && !RegisterFile.flagZ())
                     	{
 				            int[] operands = statement.getOperands();
 				            processJump(
 				               ((RegisterFile.getProgramCounter() & 0xF0000000)
 				                       | (operands[0] << 2)));
                     	}
                    }
                 }));
          instructionList.add(
                  new BasicInstruction("B.LS target", 
              	 "Branch on Unsigned Lower or Same: Jump to statement at target address if Carry is false or Zero is true; flags=xxx0||x1xx",
              	 BasicInstructionFormat.J_FORMAT,
                  "000010 ffffffffffffffffffffffffff",
                  new SimulationCode()
                 {
                     public void simulate(ProgramStatement statement) throws ProcessingException
                    {
                     	if (!RegisterFile.flagC() || RegisterFile.flagZ())
                     	{
 				            int[] operands = statement.getOperands();
 				            processJump(
 				               ((RegisterFile.getProgramCounter() & 0xF0000000)
 				                       | (operands[0] << 2)));
                     	}
                    }
                 }));
          instructionList.add(
                  new BasicInstruction("B.GE target", 
              	 "Branch on Signed Greater Than/Equal: Jump to statement at target address if Negative and oVerflow have the same value; flags=1x1x||0x0x",
              	 BasicInstructionFormat.J_FORMAT,
                  "000010 ffffffffffffffffffffffffff",
                  new SimulationCode()
                 {
                     public void simulate(ProgramStatement statement) throws ProcessingException
                    {
                     	if (RegisterFile.flagN() == RegisterFile.flagV())
                     	{
 				            int[] operands = statement.getOperands();
 				            processJump(
 				               ((RegisterFile.getProgramCounter() & 0xF0000000)
 				                       | (operands[0] << 2)));
                     	}
                    }
                 }));
          instructionList.add(
                  new BasicInstruction("B.GT target", 
              	 "Branch on Signed Greater Than: Jump to statement at target address if Negative and oVerflow have the same value, and Zero is false; flags=101x||000x",
              	 BasicInstructionFormat.J_FORMAT,
                  "000010 ffffffffffffffffffffffffff",
                  new SimulationCode()
                 {
                     public void simulate(ProgramStatement statement) throws ProcessingException
                    {
                     	if ((RegisterFile.flagN() == RegisterFile.flagV()) && !RegisterFile.flagZ())
                     	{
 				            int[] operands = statement.getOperands();
 				            processJump(
 				               ((RegisterFile.getProgramCounter() & 0xF0000000)
 				                       | (operands[0] << 2)));
                     	}
                    }
                 }));
          instructionList.add(
                  new BasicInstruction("B.LT target", 
              	 "Branch on Signed Less Than: Jump to statement at target address if Negative and oVerflow have different values; flags=1x0x||0x1x",
              	 BasicInstructionFormat.J_FORMAT,
                  "000010 ffffffffffffffffffffffffff",
                  new SimulationCode()
                 {
                     public void simulate(ProgramStatement statement) throws ProcessingException
                    {
                     	if (RegisterFile.flagN() != RegisterFile.flagV())
                     	{
 				            int[] operands = statement.getOperands();
 				            processJump(
 				               ((RegisterFile.getProgramCounter() & 0xF0000000)
 				                       | (operands[0] << 2)));
                     	}
                    }
                 }));
          instructionList.add(
                  new BasicInstruction("B.LE target", 
              	 "Branch on Signed Less Than/Equal: Jump to statement at target address if Negative and oVerflow have different values, or if Zero is true; flags=1x0x||0x1x||x1xx",
              	 BasicInstructionFormat.J_FORMAT,
                  "000010 ffffffffffffffffffffffffff",
                  new SimulationCode()
                 {
                     public void simulate(ProgramStatement statement) throws ProcessingException
                    {
                      	if ((RegisterFile.flagN() != RegisterFile.flagV()) || RegisterFile.flagZ())
                     	{
 				            int[] operands = statement.getOperands();
 				            processJump(
 				               ((RegisterFile.getProgramCounter() & 0xF0000000)
 				                       | (operands[0] << 2)));
                     	}
                    }
                 }));
         instructionList.add(
                new BasicInstruction("SVC 0", 
            	 "Issue a system call : Execute the system call specified by value in X8",
              	 BasicInstructionFormat.J_FORMAT,
                 "001100 ffffffffffffffffffffffffff",
                new SimulationCode()
               {
                   public void simulate(ProgramStatement statement) throws ProcessingException
                  {
                	 // We're using X8 because that's what is specified online, so far as I'm aware.
                     findAndSimulateSyscall(RegisterFile.getValue(8),statement);
                  }
               }));
          instructionList.add(
                 new BasicInstruction("LDXR X1,[X2,-100]",
                 "Load Exclusive Register: Paired with Store Exclusive Register (STXR) to perform atomic read-modify-write.  Treated as equivalent to Load Register (LDUR) because AARS does not simulate multiple processors.",
             	 BasicInstructionFormat.I_FORMAT,
                 "110000 ttttt fffff ssssssssssssssss",
             	 // The ll (load link) command is supposed to be the front end of an atomic
             	 // operation completed by sc (store conditional), with success or failure
             	 // of the store depending on whether the memory block containing the
             	 // loaded word is modified in the meantime by a different processor.
             	 // Since MARS, like SPIM simulates only a single processor, the store
             	 // conditional will always succeed so there is no need to do anything
             	 // special here.  In that case, ll is same as lw.  And sc does the same
             	 // thing as sw except in addition it writes 1 into the source register.
                 new SimulationCode()
                {
                    public void simulate(ProgramStatement statement) throws ProcessingException
                   {
                      int[] operands = statement.getOperands();
                      try
                      {
                         RegisterFile.updateRegister(operands[0],
                             Globals.memory.getWord(
                             RegisterFile.getValue(operands[1]) + operands[2]));
                      } 
                          catch (AddressErrorException e)
                         {
                            throw new ProcessingException(statement, e);
                         }
                   }
                }));
          instructionList.add(
                 new BasicInstruction("STXR X1,X3,[X2]",
                 "Store Exclusive Register: Paired with Load Exclusive Register (LDXR) to perform atomic read-modify-write.  Stores X1 value into the address in X2, then sets X3 to 0 for success.  Always succeeds because AARS does not simulate multiple processors.",
             	 BasicInstructionFormat.I_FORMAT,
                 "111000 ttttt fffff ssssssssssssssss",
             	 // See comments with "ll" instruction above.  "sc" is implemented
             	 // like "sw", except that 1 is placed in the source register.
                 new SimulationCode()
                {
                    public void simulate(ProgramStatement statement) throws ProcessingException
                   {
                      int[] operands = statement.getOperands();
                      try
                      {
                         Globals.memory.setWord(
                             RegisterFile.getValue(operands[2]),
                             RegisterFile.getValue(operands[0]));
                      } 
                          catch (AddressErrorException e)
                         {
                            throw new ProcessingException(statement, e);
                         }
                      RegisterFile.updateRegister(operands[1],0); // always succeeds
                   }
                }));
          instructionList.add(
                  new BasicInstruction("LDUR X1, [X2,-100]",
              	 "Load word : Set X1 to contents of effective memory word address",
                  BasicInstructionFormat.I_FORMAT,
                  "100011 ttttt fffff ssssssssssssssss",
                  new SimulationCode()
                 {
                     public void simulate(ProgramStatement statement) throws ProcessingException
                    {
                       int[] operands = statement.getOperands();
                       try
                       {
                          RegisterFile.updateRegister(operands[0],
                              Globals.memory.getWord(
                              RegisterFile.getValue(operands[1]) + operands[2]));
                       } 
                           catch (AddressErrorException e)
                          {
                             throw new ProcessingException(statement, e);
                          }
                    }
                 }));
          instructionList.add(
                  new BasicInstruction("STUR X1,[X2,-100]",
                  "Store word : Store contents of X1 into effective memory word address",
              	 BasicInstructionFormat.I_FORMAT,
                  "101011 ttttt fffff ssssssssssssssss",
                  new SimulationCode()
                 {
                     public void simulate(ProgramStatement statement) throws ProcessingException
                    {
                       int[] operands = statement.getOperands();
                       try
                       {
                          Globals.memory.setWord(
                              RegisterFile.getValue(operands[1]) + operands[2],
                              RegisterFile.getValue(operands[0]));
                       } 
                           catch (AddressErrorException e)
                          {
                             throw new ProcessingException(statement, e);
                          }
                    }
                 }));
          instructionList.add(
                  new BasicInstruction("LDA X1,label",
                  "Load Address: Convenience instruction that sets X1 to the value of the label",
              	 BasicInstructionFormat.I_FORMAT,
                  "001111 00000 fffff ssssssssssssssss",
                  new SimulationCode()
                 {
                     public void simulate(ProgramStatement statement) throws ProcessingException
                    {
                       int[] operands = statement.getOperands();
                       RegisterFile.updateRegister(operands[0], operands[1]);
                    }
                 }));
          instructionList.add(
                 new BasicInstruction("MOVZ X1,100,LSL 0",
                 "Move wide with zero: Set 16 bits of X1 to 16-bit immediate and zero the rest. Placement is determined by LSL; valid inputs 0-3. Currently ignores the MSB of LSL.",
             	 BasicInstructionFormat.I_FORMAT,
                 "001111 ttttt fffff ssssssssssssssss",
                 new SimulationCode()
                {
                    public void simulate(ProgramStatement statement) throws ProcessingException
                   {
                      int[] operands = statement.getOperands();
                      // data validation for LSL
                      if (operands[2]>3) {
                          throw new ProcessingException(statement, "Invalid value for LSL");
                       }
                      // TODO: this will need adjustment for 64bit
                      int imm16 = operands[1] << 16 >> 16;
                      int lsl = (operands[2] << 31 >> 31)*16; // LSL denotes which halfword to shift the value into
                      RegisterFile.updateRegister(operands[0], imm16 << lsl);
                   }
                }));
          instructionList.add(
                  new BasicInstruction("MOVK X1,100,LSL 0",
                  "Move wide with keep: Set 16 bits of X1 to 16-bit immediate and keep the rest. Placement is determined by LSL; valid inputs 0-3. Currently ignores the MSB of LSL.",
              	 BasicInstructionFormat.I_FORMAT,
                  "001111 ttttt fffff ssssssssssssssss",
                  new SimulationCode()
                 {
                     public void simulate(ProgramStatement statement) throws ProcessingException
                    {
                       int[] operands = statement.getOperands();
                       // data validation for LSL
                       if (operands[2]>3) {
                           throw new ProcessingException(statement, "Invalid value for LSL");
                        }
                       // TODO: this will need adjustment for 64bit
                       int imm16 = operands[1] << 16 >> 16;
                       int lsl = (operands[2] << 31 >> 31)*16; // LSL denotes which halfword to shift the value into
                       int mask = Integer.MAX_VALUE ^ (Short.MAX_VALUE << lsl);
                       int keptBits = RegisterFile.getValue(operands[0]) & mask;
                       int result = keptBits | (imm16 << lsl);
                       RegisterFile.updateRegister(operands[0], result);
                    }
                 }));
          instructionList.add(
                  new BasicInstruction("MOVZ X1,100,0",
                  "Move wide with zero: Set 16 bits of X1 to 16-bit immediate and zero the rest. Placement is determined by LSL; valid inputs 0-3. Currently ignores the MSB of LSL.",
              	 BasicInstructionFormat.I_FORMAT,
                  "001111 ttttt fffff ssssssssssssssss",
                  new SimulationCode()
                 {
                     public void simulate(ProgramStatement statement) throws ProcessingException
                    {
                       int[] operands = statement.getOperands();
                       // data validation for LSL
                       if (operands[2]>3) {
                           throw new ProcessingException(statement, "Invalid value for LSL");
                        }
                       // TODO: this will need adjustment for 64bit
                       int imm16 = operands[1] << 16 >> 16;
                       int lsl = (operands[2] << 31 >> 31)*16; // LSL denotes which halfword to shift the value into
                       RegisterFile.updateRegister(operands[0], imm16 << lsl);
                    }
                 }));
           instructionList.add(
                   new BasicInstruction("MOVK X1,100,0",
                   "Move wide with keep: Set 16 bits of X1 to 16-bit immediate and keep the rest. Placement is determined by LSL; valid inputs 0-3. Currently ignores the MSB of LSL.",
               	 BasicInstructionFormat.I_FORMAT,
                   "001111 ttttt fffff ssssssssssssssss",
                   new SimulationCode()
                  {
                      public void simulate(ProgramStatement statement) throws ProcessingException
                     {
                        int[] operands = statement.getOperands();
                        // data validation for LSL
                        if (operands[2]>3) {
                            throw new ProcessingException(statement, "Invalid value for LSL");
                         }
                        // TODO: this will need adjustment for 64bit
                        int imm16 = operands[1] << 16 >> 16;
                        int lsl = (operands[2] << 31 >> 31)*16; // LSL denotes which halfword to shift the value into
                        int mask = Integer.MAX_VALUE ^ (Short.MAX_VALUE << lsl);
                        int keptBits = RegisterFile.getValue(operands[0]) & mask;
                        int result = keptBits | (imm16 << lsl);
                        RegisterFile.updateRegister(operands[0], result);
                     }
                  }));
         instructionList.add(
                 new BasicInstruction("LDURB X1,[X2,-100]",
        		 "Load byte unsigned : Set X1 to zero-extended 8-bit value from effective memory byte address",
        		 BasicInstructionFormat.I_FORMAT,
        		 "100100 ttttt fffff ssssssssssssssss",
        		 new SimulationCode()
        		{
        		    public void simulate(ProgramStatement statement) throws ProcessingException
        		   {
        		      int[] operands = statement.getOperands();
        		      try
        		      {
        		         RegisterFile.updateRegister(operands[0],
        		             Globals.memory.getByte(
        		             RegisterFile.getValue(operands[1])
        		                     + (operands[2] << 16 >> 16))
        		                             & 0x000000ff);
        		      } 
        		          catch (AddressErrorException e)
        		         {
        		            throw new ProcessingException(statement, e);
        		         }
        		   }
        		}));
         instructionList.add(
                 new BasicInstruction("LDURH X1,[X2,-100]",
        	     "Load halfword unsigned : Set X1 to zero-extended 16-bit value from effective memory halfword address",
        	   	 BasicInstructionFormat.I_FORMAT,
        	     "100101 ttttt fffff ssssssssssssssss",
        	     new SimulationCode()
        	    {
        	        public void simulate(ProgramStatement statement) throws ProcessingException
        	       {
        	          int[] operands = statement.getOperands();
        	          try
        	          {
        	          // offset is sign-extended and loaded halfword value is zero-extended
        	             RegisterFile.updateRegister(operands[0],
        	                 Globals.memory.getHalf(
        	                 RegisterFile.getValue(operands[1])
        	                         + (operands[2] << 16 >> 16))
        	                                 & 0x0000ffff);
        	          } 
        	              catch (AddressErrorException e)
        	             {
        	                throw new ProcessingException(statement, e);
        	             }
        	       }
        	    }));
         instructionList.add(
                 new BasicInstruction("STURB X1,[X2,-100]",
                "Store byte : Store the low-order 8 bits of X1 into the effective memory byte address",
            	 BasicInstructionFormat.I_FORMAT,
                "101000 ttttt fffff ssssssssssssssss",
                new SimulationCode()
               {
                   public void simulate(ProgramStatement statement) throws ProcessingException
                  {
                     int[] operands = statement.getOperands();
                     try
                     {
                        Globals.memory.setByte(
                            RegisterFile.getValue(operands[1])
                                    + (operands[2] << 16 >> 16),
                                    RegisterFile.getValue(operands[0])
                                            & 0x000000ff);
                     } 
                         catch (AddressErrorException e)
                        {
                           throw new ProcessingException(statement, e);
                        }
                  }
               }));
         instructionList.add(
                 new BasicInstruction("STURH X1,[X2,-100]",
                "Store halfword : Store the low-order 16 bits of X1 into the effective memory halfword address",
            	 BasicInstructionFormat.I_FORMAT,
                "101001 ttttt fffff ssssssssssssssss",
                new SimulationCode()
               {
                   public void simulate(ProgramStatement statement) throws ProcessingException
                  {
                     int[] operands = statement.getOperands();
                     try
                     {
                        Globals.memory.setHalf(
                            RegisterFile.getValue(operands[1])
                                    + (operands[2] << 16 >> 16),
                                    RegisterFile.getValue(operands[0])
                                            & 0x0000ffff);
                     } 
                         catch (AddressErrorException e)
                        {
                           throw new ProcessingException(statement, e);
                        }
                  }
               }));
      			
        /////////////////////// Floating Point Instructions Start Here ////////////////
         instructionList.add(
                new BasicInstruction("FADDS S0,S1,S3",
                "Floating point addition single precision : Set S0 to single-precision floating point value of S1 plus S3", 
            	 BasicInstructionFormat.R_FORMAT,
                "010001 10000 ttttt sssss fffff 000000",
                new SimulationCode()
               {
                   public void simulate(ProgramStatement statement) throws ProcessingException
                  { 
                     int[] operands = statement.getOperands();
                     float add1 = Float.intBitsToFloat(Coprocessor1.getValue(operands[1]));
                     float add2 = Float.intBitsToFloat(Coprocessor1.getValue(operands[2]));
                     float sum = add1 + add2;
                  // overflow detected when sum is positive or negative infinity.
                  /*
                  if (sum == Float.NEGATIVE_INFINITY || sum == Float.POSITIVE_INFINITY) {
                    throw new ProcessingException(statement,"arithmetic overflow");
                  }
                  */
                     Coprocessor1.updateRegister(operands[0], Float.floatToIntBits(sum));
                  }
               }));
         instructionList.add(
                new BasicInstruction("FSUBS S0,S1,S3",
                "Floating point subtraction single precision : Set S0 to single-precision floating point value of S1  minus S3",
            	 BasicInstructionFormat.R_FORMAT,
                "010001 10000 ttttt sssss fffff 000001",
                new SimulationCode()
               {
                   public void simulate(ProgramStatement statement) throws ProcessingException
                  { 
                     int[] operands = statement.getOperands();
                     float sub1 = Float.intBitsToFloat(Coprocessor1.getValue(operands[1]));
                     float sub2 = Float.intBitsToFloat(Coprocessor1.getValue(operands[2]));
                     float diff = sub1 - sub2;
                     Coprocessor1.updateRegister(operands[0], Float.floatToIntBits(diff));
                  }
               }));
         instructionList.add(
                new BasicInstruction("FMULS S0,S1,S3",
                "Floating point multiplication single precision : Set S0 to single-precision floating point value of S1 times S3",
            	 BasicInstructionFormat.R_FORMAT,
                "010001 10000 ttttt sssss fffff 000010",
                new SimulationCode()
               {
                   public void simulate(ProgramStatement statement) throws ProcessingException
                  { 
                     int[] operands = statement.getOperands();
                     float mul1 = Float.intBitsToFloat(Coprocessor1.getValue(operands[1]));
                     float mul2 = Float.intBitsToFloat(Coprocessor1.getValue(operands[2]));
                     float prod = mul1 * mul2;
                     Coprocessor1.updateRegister(operands[0], Float.floatToIntBits(prod));
                  }
               }));
         instructionList.add(
                new BasicInstruction("FDIVS S0,S1,S3",
                "Floating point division single precision : Set S0 to single-precision floating point value of S1 divided by S3",
            	 BasicInstructionFormat.R_FORMAT,
                "010001 10000 ttttt sssss fffff 000011",
                new SimulationCode()
               {
                   public void simulate(ProgramStatement statement) throws ProcessingException
                  { 
                     int[] operands = statement.getOperands();
                     float div1 = Float.intBitsToFloat(Coprocessor1.getValue(operands[1]));
                     float div2 = Float.intBitsToFloat(Coprocessor1.getValue(operands[2]));
                     float quot = div1 / div2;
                     Coprocessor1.updateRegister(operands[0], Float.floatToIntBits(quot));
                  }
               }));
         instructionList.add(
                new BasicInstruction("FADDD D0,D1,D2",
            	 "Floating point addition double precision : Set D2 to double-precision floating point value of D4 plus D6",
            	 BasicInstructionFormat.R_FORMAT,
                "010001 10001 ttttt sssss fffff 000000",
                new SimulationCode()
               {
                   public void simulate(ProgramStatement statement) throws ProcessingException
                  { 
                     int[] operands = statement.getOperands();
                     if (operands[0]%2==1 || operands[1]%2==1 || operands[2]%2==1) {
                        throw new ProcessingException(statement, DOUBLE_SIZE_REGISTER_ERROR);
                     }
                     double add1 = Double.longBitsToDouble(Binary.twoIntsToLong(
                              Coprocessor1.getValue(operands[1]+1),Coprocessor1.getValue(operands[1])));
                     double add2 = Double.longBitsToDouble(Binary.twoIntsToLong(
                              Coprocessor1.getValue(operands[2]+1),Coprocessor1.getValue(operands[2])));
                     double sum  = add1 + add2;
                     long longSum = Double.doubleToLongBits(sum);
                     Coprocessor1.updateRegister(operands[0]+1, Binary.highOrderLongToInt(longSum));
                     Coprocessor1.updateRegister(operands[0], Binary.lowOrderLongToInt(longSum));
                  }
               }));
         instructionList.add(
                new BasicInstruction("FSUBD D2,D4,D6",
            	 "Floating point subtraction double precision : Set D2 to double-precision floating point value of D4 minus D6",
                BasicInstructionFormat.R_FORMAT,
                "010001 10001 ttttt sssss fffff 000001",
                new SimulationCode()
               {
                   public void simulate(ProgramStatement statement) throws ProcessingException
                  { 
                     int[] operands = statement.getOperands();
                     if (operands[0]%2==1 || operands[1]%2==1 || operands[2]%2==1) {
                        throw new ProcessingException(statement, DOUBLE_SIZE_REGISTER_ERROR);
                     }
                     double sub1 = Double.longBitsToDouble(Binary.twoIntsToLong(
                              Coprocessor1.getValue(operands[1]+1),Coprocessor1.getValue(operands[1])));
                     double sub2 = Double.longBitsToDouble(Binary.twoIntsToLong(
                              Coprocessor1.getValue(operands[2]+1),Coprocessor1.getValue(operands[2])));
                     double diff = sub1 - sub2;
                     long longDiff = Double.doubleToLongBits(diff);
                     Coprocessor1.updateRegister(operands[0]+1, Binary.highOrderLongToInt(longDiff));
                     Coprocessor1.updateRegister(operands[0], Binary.lowOrderLongToInt(longDiff));
                  }
               }));
         instructionList.add(
                new BasicInstruction("FMULD D2,D4,D6",
            	 "Floating point multiplication double precision : Set D2 to double-precision floating point value of D4 times D6",
                BasicInstructionFormat.R_FORMAT,
                "010001 10001 ttttt sssss fffff 000010",
                new SimulationCode()
               {
                   public void simulate(ProgramStatement statement) throws ProcessingException
                  { 
                     int[] operands = statement.getOperands();
                     if (operands[0]%2==1 || operands[1]%2==1 || operands[2]%2==1) {
                        throw new ProcessingException(statement, DOUBLE_SIZE_REGISTER_ERROR);
                     }
                     double mul1 = Double.longBitsToDouble(Binary.twoIntsToLong(
                              Coprocessor1.getValue(operands[1]+1),Coprocessor1.getValue(operands[1])));
                     double mul2 = Double.longBitsToDouble(Binary.twoIntsToLong(
                              Coprocessor1.getValue(operands[2]+1),Coprocessor1.getValue(operands[2])));
                     double prod  = mul1 * mul2;
                     long longProd = Double.doubleToLongBits(prod);
                     Coprocessor1.updateRegister(operands[0]+1, Binary.highOrderLongToInt(longProd));
                     Coprocessor1.updateRegister(operands[0], Binary.lowOrderLongToInt(longProd));
                  }
               }));
         instructionList.add(
                new BasicInstruction("FDIVD D2,D4,D6",
            	 "Floating point division double precision : Set D2 to double-precision floating point value of D4 divided by D6",
                BasicInstructionFormat.R_FORMAT,
                "010001 10001 ttttt sssss fffff 000011",
                new SimulationCode()
               {
                   public void simulate(ProgramStatement statement) throws ProcessingException
                  { 
                     int[] operands = statement.getOperands();
                     if (operands[0]%2==1 || operands[1]%2==1 || operands[2]%2==1) {
                        throw new ProcessingException(statement, DOUBLE_SIZE_REGISTER_ERROR);
                     }
                     double div1 = Double.longBitsToDouble(Binary.twoIntsToLong(
                              Coprocessor1.getValue(operands[1]+1),Coprocessor1.getValue(operands[1])));
                     double div2 = Double.longBitsToDouble(Binary.twoIntsToLong(
                              Coprocessor1.getValue(operands[2]+1),Coprocessor1.getValue(operands[2])));
                     double quot  = div1 / div2;
                     long longQuot = Double.doubleToLongBits(quot);
                     Coprocessor1.updateRegister(operands[0]+1, Binary.highOrderLongToInt(longQuot));
                     Coprocessor1.updateRegister(operands[0], Binary.lowOrderLongToInt(longQuot));
                  }
               }));
         instructionList.add(
                 new BasicInstruction("LDURS S1, [X2,-100]",
                 "Load Floating Point Single : Set S1 to 32-bit value from effective memory word address",
             	 BasicInstructionFormat.I_FORMAT,
                 "110001 ttttt fffff ssssssssssssssss",
                 new SimulationCode()
                {
                    public void simulate(ProgramStatement statement) throws ProcessingException
                   { 
                      int[] operands = statement.getOperands();
                      try
                      {
                         Coprocessor1.updateRegister(operands[0],
                             Globals.memory.getWord(
                             RegisterFile.getValue(operands[1]) + operands[2]));
                      } 
                          catch (AddressErrorException e)
                         {
                            throw new ProcessingException(statement, e);
                         }
                   }
                }));		 
          instructionList.add(
                  new BasicInstruction("LDURD D1, [X2,-100]",
             	 "Load Floating Point Double : Set D1 to 64-bit value from effective memory doubleword address",
                 BasicInstructionFormat.I_FORMAT,
                 "110101 ttttt fffff ssssssssssssssss",
                 new SimulationCode()
                {
                    public void simulate(ProgramStatement statement) throws ProcessingException
                   { 
                      int[] operands = statement.getOperands();
                      if (operands[0]%2==1) {
                         throw new ProcessingException(statement, DOUBLE_SIZE_REGISTER_ERROR);
                      }
                   	// IF statement added by DPS 13-July-2011.
                      if (!Globals.memory.doublewordAligned(RegisterFile.getValue(operands[1]) + operands[2])) {
                         throw new ProcessingException(statement,
                            new AddressErrorException("address not aligned on doubleword boundary ",
                            Exceptions.ADDRESS_EXCEPTION_LOAD, RegisterFile.getValue(operands[1]) + operands[2]));
                      }
                                     
                      try
                      {
                         Coprocessor1.updateRegister(operands[0],
                             Globals.memory.getWord(
                             RegisterFile.getValue(operands[1]) + operands[2]));
                         Coprocessor1.updateRegister(operands[0]+1,
                             Globals.memory.getWord(
                             RegisterFile.getValue(operands[1]) + operands[2] + 4));
                      } 
                          catch (AddressErrorException e)
                         {
                            throw new ProcessingException(statement, e);
                         }
                   }
                }));	 
          instructionList.add(
                  new BasicInstruction("STURS S1, [X2,-100]",
             	 "Store Floating Point Single : Store 32 bit value in S1 to effective memory word address",
                 BasicInstructionFormat.I_FORMAT,
                 "111001 ttttt fffff ssssssssssssssss",
                 new SimulationCode()
                {
                    public void simulate(ProgramStatement statement) throws ProcessingException
                   { 
                      int[] operands = statement.getOperands();
                      try
                      {
                         Globals.memory.setWord(
                             RegisterFile.getValue(operands[1]) + operands[2],
                             Coprocessor1.getValue(operands[0]));
                      } 
                          catch (AddressErrorException e)
                         {
                            throw new ProcessingException(statement, e);
                         }
                   }
                }));
          instructionList.add(
                  new BasicInstruction("STURD D1, [X2,-100]",
             	 "Store Floating Point Double : Store 64 bit value in D1 to effective memory doubleword address",
                 BasicInstructionFormat.I_FORMAT,
                 "111101 ttttt fffff ssssssssssssssss",
                 new SimulationCode()
                {
                    public void simulate(ProgramStatement statement) throws ProcessingException
                   { 
                      int[] operands = statement.getOperands();
                      if (operands[0]%2==1) {
                         throw new ProcessingException(statement, DOUBLE_SIZE_REGISTER_ERROR);
                      }
                   	// IF statement added by DPS 13-July-2011.
                      if (!Globals.memory.doublewordAligned(RegisterFile.getValue(operands[1]) + operands[2])) {
                         throw new ProcessingException(statement,
                            new AddressErrorException("address not aligned on doubleword boundary ",
                            Exceptions.ADDRESS_EXCEPTION_STORE, RegisterFile.getValue(operands[1]) + operands[2]));
                      }
                      try
                      {
                         Globals.memory.setWord(
                             RegisterFile.getValue(operands[1]) + operands[2],
                             Coprocessor1.getValue(operands[0]));
                         Globals.memory.setWord(
                             RegisterFile.getValue(operands[1]) + operands[2] + 4,
                             Coprocessor1.getValue(operands[0]+1));
                      } 
                          catch (AddressErrorException e)
                         {
                            throw new ProcessingException(statement, e);
                         }
                   }
                }));
         instructionList.add(
                new BasicInstruction("FCMPS S0,S1",
                "Floating point compare single : If equal, flags=0110. If less than, flags=1000. If greater than, flags=0010. If either operand is invalid, flags=0001.",
            	BasicInstructionFormat.R_FORMAT,
                "010001 10000 sssss fffff 00000 110010",
                new SimulationCode()
               {
                   public void simulate(ProgramStatement statement) throws ProcessingException
                  { 
                     int[] operands = statement.getOperands();
                     float op1 = Float.intBitsToFloat(Coprocessor1.getValue(operands[0]));
                     float op2 = Float.intBitsToFloat(Coprocessor1.getValue(operands[1]));
                     
                     if (Float.isNaN(op1) || Float.isNaN(op2))
                     {
                    	 // flags=0001; not negative, nor zero, no overflow, but carry
                    	 RegisterFile.setFlags(1, false, true);
                     }
                     else
                     {
	                     if (op1 == op2)
	                    	// flags=0110; not negative, zero, overflow, no carry
	                    	RegisterFile.setFlags(0, true, false);
	                     if (op1 < op2)
	                    	// flags=1000; negative, not zero, no overflow, no carry
	                    	RegisterFile.setFlags(-1, false, false);
	                     if (op1 > op2)
	                    	// flags=0010; not negative, not zero, overflow, no carry
	                    	RegisterFile.setFlags(1, true, false);
                     }
                  }
               }));
         instructionList.add(
                new BasicInstruction("FCMPD D0,D1",
                "Floating point compare double : If equal, flags=0110. If less than, flags=1000. If greater than, flags=0010. If either operand is invalid, flags=0001.",
                BasicInstructionFormat.R_FORMAT,
                "010001 10001 sssss fffff 00000 110010",
                new SimulationCode()
               {
                   public void simulate(ProgramStatement statement) throws ProcessingException
                  { 
                     int[] operands = statement.getOperands();
                     if (operands[0]%2==1 || operands[1]%2==1) {
                        throw new ProcessingException(statement, DOUBLE_SIZE_REGISTER_ERROR);
                     }
                     double op1 = Double.longBitsToDouble(Binary.twoIntsToLong(
                              Coprocessor1.getValue(operands[0]+1),Coprocessor1.getValue(operands[0])));
                     double op2 = Double.longBitsToDouble(Binary.twoIntsToLong(
                              Coprocessor1.getValue(operands[1]+1),Coprocessor1.getValue(operands[1])));

                     if (Double.isNaN(op1) || Double.isNaN(op2))
                     {
                    	 // flags=0001; not negative, nor zero, no overflow, but carry
                    	 RegisterFile.setFlags(1, false, true);
                     }
                     else
                     {
	                     if (op1 == op2)
	                    	// flags=0110; not negative, zero, overflow, no carry
	                    	RegisterFile.setFlags(0, true, false);
	                     if (op1 < op2)
	                    	// flags=1000; negative, not zero, no overflow, no carry
	                    	RegisterFile.setFlags(-1, false, false);
	                     if (op1 > op2)
	                    	// flags=0010; not negative, not zero, overflow, no carry
	                    	RegisterFile.setFlags(1, true, false);
                     }
                  }
               }));
      	
        ////////////// GET AND CREATE LIST OF SYSCALL FUNCTION OBJECTS ////////////////////
         syscallLoader = new SyscallLoader();
         syscallLoader.loadSyscalls();
      	
        // Initialization step.  Create token list for each instruction example.  This is
        // used by parser to determine user program correct syntax.
         for (int i = 0; i < instructionList.size(); i++)
         {
            Instruction inst = (Instruction) instructionList.get(i);
            inst.createExampleTokenList();
         }

		 HashMap maskMap = new HashMap();
		 ArrayList matchMaps = new ArrayList();
		 for (int i = 0; i < instructionList.size(); i++) {
		 	Object rawInstr = instructionList.get(i);
			if (rawInstr instanceof BasicInstruction) {
				BasicInstruction basic = (BasicInstruction) rawInstr;
				Integer mask = Integer.valueOf(basic.getOpcodeMask());
				Integer match = Integer.valueOf(basic.getOpcodeMatch());
				HashMap matchMap = (HashMap) maskMap.get(mask);
				if (matchMap == null) {
					matchMap = new HashMap();
					maskMap.put(mask, matchMap);
					matchMaps.add(new MatchMap(mask, matchMap));
				}
				matchMap.put(match, basic);
			}
		 }
		 Collections.sort(matchMaps);
		 this.opcodeMatchMaps = matchMaps;
      }

	public BasicInstruction findByBinaryCode(int binaryInstr) {
		ArrayList matchMaps = this.opcodeMatchMaps;
		for (int i = 0; i < matchMaps.size(); i++) {
			MatchMap map = (MatchMap) matchMaps.get(i);
			BasicInstruction ret = map.find(binaryInstr);
			if (ret != null) return ret;
		}
		return null;
	}
   	
   	
    /**
     *  Given an operator mnemonic, will return the corresponding Instruction object(s)
     *  from the instruction set.  Uses straight linear search technique.
     *  @param name operator mnemonic (e.g. addi, sw,...)
     *  @return list of corresponding Instruction object(s), or null if not found.
     */
       public ArrayList<Instruction> matchOperator(String name)
      {
         ArrayList<Instruction> matchingInstructions = null;
        // Linear search for now....
         for (int i = 0; i < instructionList.size(); i++)
         {
            if (((Instruction) instructionList.get(i)).getName().equalsIgnoreCase(name))
            {
               if (matchingInstructions == null) 
                  matchingInstructions = new ArrayList<Instruction>();
               matchingInstructions.add(instructionList.get(i));
            }
         }
         return matchingInstructions;
      }
   
   
    /**
     *  Given a string, will return the Instruction object(s) from the instruction
     *  set whose operator mnemonic prefix matches it.  Case-insensitive.  For example
     *  "s" will match "sw", "sh", "sb", etc.  Uses straight linear search technique.
     *  @param name a string
     *  @return list of matching Instruction object(s), or null if none match.
     */
       public ArrayList<Instruction> prefixMatchOperator(String name)
      {
         ArrayList<Instruction> matchingInstructions = null;
        // Linear search for now....
         if (name != null) {
            for (int i = 0; i < instructionList.size(); i++)
            {
               if (((Instruction) instructionList.get(i)).getName().toLowerCase().startsWith(name.toLowerCase()))
               {
                  if (matchingInstructions == null) 
                     matchingInstructions = new ArrayList<Instruction>();
                  matchingInstructions.add(instructionList.get(i));
               }
            }
         }
         return matchingInstructions;
      }
   	
   	/*
   	 * Method to find and invoke a syscall given its service number.  Each syscall
   	 * function is represented by an object in an array list.  Each object is of
   	 * a class that implements Syscall or extends AbstractSyscall.
   	 */
   	 
       private void findAndSimulateSyscall(int number, ProgramStatement statement) 
                                                        throws ProcessingException {
         Syscall service = syscallLoader.findSyscall(number);
         if (service != null) {
            service.simulate(statement);
            return;
         }
         throw new ProcessingException(statement,
              "invalid or unimplemented syscall service: " +
              number + " ", Exceptions.SYSCALL_EXCEPTION);
      }
   	
   	/*
   	 * Method to process a successful branch condition.  DO NOT USE WITH JUMP
   	 * INSTRUCTIONS!  The branch operand is a relative displacement in words
   	 * whereas the jump operand is an absolute address in bytes.
   	 *
   	 * The parameter is displacement operand from instruction.
   	 *
   	 * Handles delayed branching if that setting is enabled.
   	 */
   	 // 4 January 2008 DPS:  The subtraction of 4 bytes (instruction length) after
   	 // the shift has been removed.  It is left in as commented-out code below.
   	 // This has the effect of always branching as if delayed branching is enabled, 
   	 // even if it isn't.  This mod must work in conjunction with
   	 // ProgramStatement.java, buildBasicStatementFromBasicInstruction() method near
   	 // the bottom (currently line 194, heavily commented).
   	 
       private void processBranch(int displacement) {
         if (Globals.getSettings().getDelayedBranchingEnabled()) {
            // Register the branch target address (absolute byte address).
            DelayedBranch.register(RegisterFile.getProgramCounter() + (displacement << 2));
         } 
         else {
            // Decrement needed because PC has already been incremented
            RegisterFile.setProgramCounter(
                RegisterFile.getProgramCounter()
                  + (displacement << 2)); // - Instruction.INSTRUCTION_LENGTH);	
         }	 
      }
   
   	/*
   	 * Method to process a jump.  DO NOT USE WITH BRANCH INSTRUCTIONS!  
   	 * The branch operand is a relative displacement in words
   	 * whereas the jump operand is an absolute address in bytes.
   	 *
   	 * The parameter is jump target absolute byte address.
   	 *
   	 * Handles delayed branching if that setting is enabled.
   	 */
   	 
       private void processJump(int targetAddress) {
         if (Globals.getSettings().getDelayedBranchingEnabled()) {
            DelayedBranch.register(targetAddress);
         } 
         else {
            RegisterFile.setProgramCounter(targetAddress);
         }	 
      }
   
   	/*
   	 * Method to process storing of a return address in the given
   	 * register.  This is used only by the "and link"
   	 * instructions: jal, jalr, bltzal, bgezal.  If delayed branching
   	 * setting is off, the return address is the address of the
   	 * next instruction (e.g. the current PC value).  If on, the
   	 * return address is the instruction following that, to skip over
   	 * the delay slot.
   	 *
   	 * The parameter is register number to receive the return address.
   	 */
   	 
       private void processReturnAddress(int register) {
         RegisterFile.updateRegister(register, RegisterFile.getProgramCounter() +
                 ((Globals.getSettings().getDelayedBranchingEnabled()) ? 
            	  Instruction.INSTRUCTION_LENGTH : 0) );	 
      }

	  private static class MatchMap implements Comparable {
	  	private int mask;
		private int maskLength; // number of 1 bits in mask
		private HashMap matchMap;

		public MatchMap(int mask, HashMap matchMap) {
			this.mask = mask;
			this.matchMap = matchMap;

			int k = 0;
			int n = mask;
			while (n != 0) {
				k++;
				n &= n - 1;
			}
			this.maskLength = k;
		}

		public boolean equals(Object o) {
			return o instanceof MatchMap && mask == ((MatchMap) o).mask;
		}

		public int compareTo(Object other) {
			MatchMap o = (MatchMap) other;
			int d = o.maskLength - this.maskLength;
			if (d == 0) d = this.mask - o.mask;
			return d;
		}

		public BasicInstruction find(int instr) {
			int match = Integer.valueOf(instr & mask);
			return (BasicInstruction) matchMap.get(match);
		}
	}
   }

