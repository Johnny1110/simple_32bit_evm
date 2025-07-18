package com.frizo.lab.sevm.blockchain.impl.mock;

import com.frizo.lab.sevm.common.Address;
import com.frizo.lab.sevm.op.Opcode;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;

@Slf4j
public class MockContractStorage {

    @Getter
    private static final Map<Address, byte[]> contractStorage = new HashMap<>();


    static {
        log.info("[MockBlockChain] Initialized MockBlockChain with empty contract storage.");

        // Default contracts for testing
        addContract(Address.of("0x0101010101010101"), returnNothingContract());
        addContract(Address.of("0x0202020202020202"), callAddTwoNumContract());
        addContract(Address.of("0x0101010101010102"), return0x3AContract());
    }

    public static void addContract(Address contractAddress, byte[] contractBytecode) {
        log.info("[MockContractStorage] add contract at address: {}", contractAddress);
        contractStorage.put(contractAddress, contractBytecode);
    }

    /**
     * 測試算術運算 + 儲存
     * contract Calculator {
     *     uint256 result;
     *
     *     function add(uint256 a, uint256 b) public {
     *         result = a + b;
     *     }
     *
     *     function getResult() public view returns (uint256) {
     *         return result;
     *     }
     * }
     * @return 合約的 EVM bytecode
     */
    private static byte[] callAddTwoNumContract() {
        log.info("[MockBlockChain] Loading contract code for callAddTwoNumContract");
        return new byte[]{
                // 合約入口點 - 函數選擇器解析

                // 檢查 calldata 大小 (至少需要 4 bytes 函數選擇器)
                Opcode.CALLDATASIZE.getCode(),
                Opcode.PUSH1.getCode(), 0x04,
                Opcode.LT.getCode(),
                Opcode.PUSH1.getCode(), 0x3C, // 跳轉到 revert
                Opcode.JUMPI.getCode(),

                // 載入函數選擇器 (calldata 的前 4 bytes)
                Opcode.PUSH1.getCode(), 0x00,
                Opcode.CALLDATALOAD.getCode(),
                Opcode.PUSH1.getCode(), (byte) 0xE0,  // 右移 224 bits (32-4)*8
                Opcode.SHR.getCode(),

                // 檢查是否為 add(uint256,uint256) - 0x771602f7
                Opcode.DUP1.getCode(),
                Opcode.PUSH4.getCode(), 0x77, 0x16, 0x02, (byte)0xf7,
                Opcode.EQ.getCode(),
                Opcode.PUSH1.getCode(), 0x45, // 跳轉到 add 函數
                Opcode.JUMPI.getCode(),

                // 檢查是否為 getResult() - 0xde292789
                Opcode.DUP1.getCode(),
                Opcode.PUSH4.getCode(), (byte)0xde, 0x29, 0x27, (byte)0x89,
                Opcode.EQ.getCode(),
                Opcode.PUSH1.getCode(), 0x65, // 跳轉到 getResult 函數
                Opcode.JUMPI.getCode(),

                // 如果沒有匹配的函數，則 revert
                Opcode.JUMPDEST.getCode(), // 地址 0x3C
                Opcode.PUSH1.getCode(), 0x00,
                Opcode.PUSH1.getCode(), 0x00,
                Opcode.REVERT.getCode(),

                // add(uint256 a, uint256 b) 函數實現
                Opcode.JUMPDEST.getCode(), // 地址 0x45

                // 檢查 calldata 大小 (4 + 32 + 32 = 68 bytes)
                Opcode.CALLDATASIZE.getCode(),
                Opcode.PUSH1.getCode(), 0x44, // 68 bytes
                Opcode.LT.getCode(),
                Opcode.PUSH1.getCode(), 0x3C, // 跳轉到 revert
                Opcode.JUMPI.getCode(),

                // 載入第一個參數 a (offset 4)
                Opcode.PUSH1.getCode(), 0x04,
                Opcode.CALLDATALOAD.getCode(),

                // 載入第二個參數 b (offset 36)
                Opcode.PUSH1.getCode(), 0x24,
                Opcode.CALLDATALOAD.getCode(),

                // 執行加法 a + b
                Opcode.ADD.getCode(),

                // 儲存結果到 storage slot 0
                Opcode.PUSH1.getCode(), 0x00, // storage slot 0
                Opcode.SSTORE.getCode(),

                // 返回 (無返回值)
                Opcode.PUSH1.getCode(), 0x00,
                Opcode.PUSH1.getCode(), 0x00,
                Opcode.RETURN.getCode(),

                // getResult() 函數實現
                Opcode.JUMPDEST.getCode(), // 地址 0x65

                // 從 storage slot 0 載入結果
                Opcode.PUSH1.getCode(), 0x00, // storage slot 0
                Opcode.SLOAD.getCode(),

                // 將結果存到記憶體準備返回
                Opcode.PUSH1.getCode(), 0x00, // memory offset 0
                Opcode.MSTORE.getCode(),

                // 返回 32 bytes 的數據
                Opcode.PUSH1.getCode(), 0x20, // 32 bytes
                Opcode.PUSH1.getCode(), 0x00, // memory offset 0
                Opcode.RETURN.getCode()
        };
    }

    private static byte[] return0x3AContract() {
        log.info("[MockBlockChain] Loading contract code for return0x3AContract");
        return new byte[]{
                Opcode.PUSH1.getCode(), (byte) 0x3A,  // PUSH1 170
                Opcode.PUSH1.getCode(), 0x10,  // PUSH1 16 (memory offset)
                Opcode.MSTORE.getCode(),        // MSTORE
                Opcode.PUSH1.getCode(), 0x08,  // PUSH1 8 (return size)
                Opcode.PUSH1.getCode(), 0x10,  // PUSH1 10 (return offset)
                Opcode.RETURN.getCode(),         // RETURN
                Opcode.STOP.getCode()          // STOP
        };
    }

    private static byte[] returnNothingContract() {
        log.info("[MockBlockChain] Loading contract code for returnNothingContract");
        return new byte[]{
                Opcode.PUSH1.getCode(), (byte) 0x3A,  // PUSH1 170
                Opcode.PUSH1.getCode(), (byte) 0x3A,  // PUSH1 170
                Opcode.PUSH1.getCode(), (byte) 0x3A,  // PUSH1 170
                Opcode.PUSH1.getCode(), (byte) 0x3A,  // PUSH1 170
                Opcode.STOP.getCode()
        };
    }


    public static byte[] get(Address contractAddress) {
        log.info("[MockContractStorage] Retrieving contract at address: {}", contractAddress);
        byte[] code = contractStorage.get(contractAddress);
        return code;
    }

    public static boolean exists(Address contractAddress) {
        return contractStorage.containsKey(contractAddress);
    }
}
