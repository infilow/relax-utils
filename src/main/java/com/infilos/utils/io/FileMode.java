package com.infilos.utils.io;

public enum FileMode {
    R,  // read only
    RW, // read & write
    RWS,// read & write, sync content and meta to storage device
    RWD // read & write, sync content to storage device
}
