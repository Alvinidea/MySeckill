package hcf.exp.lua;

import org.luaj.vm2.Globals;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.jse.JsePlatform;

/**
 * @author hechaofan
 * @date 2022/3/23 11:14
 *
 * Luaj是Java调用Lua的一种实现方式, 其是构建一个虚拟机解析执行Lua脚本来实现的, 这和Groovy的方式有所不同.
 * Luaj的官网: http://www.luaj.org/luaj/3.0/README.html
 */
public class TestLuaj {
    public static void main(String[] args) {
        String luastr = "print('Hello')";
        Globals globals = JsePlatform.standardGlobals();
        LuaValue chunk = globals.load(luastr);
        chunk.call();
        StringBuilder lua = new StringBuilder();
        lua.append("if (redis.call('exists', KEYS[1]) == 1) then");
        lua.append("    local stock = tonumber(redis.call('get', KEYS[1]));");
        lua.append("    if (stock == -1) then");
        lua.append("        return 1;");
        lua.append("    end;");
        lua.append("    if (stock > 0) then");
        lua.append("        redis.call('incrby', KEYS[1], -1);");
        lua.append("        return stock;");
        lua.append("    end;");
        lua.append("    return 0;");
        lua.append("end;");
        lua.append("return -1;");
    }
}
