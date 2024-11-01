import Cookies from "js-cookie";

const TokenKey: string = "Token";

// token前缀
export let token_prefix = "Bearer ";

// 获取 Token
export function getToken(): string | undefined {
    const token = Cookies.get(TokenKey);
    console.log('Get Token:', token); // 调试输出
    return token;
}

// 设置 Token
export function setToken(token: string): void {
    const options = {expires: 1}; // 不设置 domain 属性，使 cookie 在任何域名下都有效
    Cookies.set(TokenKey, token, options);
    console.log('Set Token:', Cookies.get()); // 调试输出
}

// 删除 Token
export function removeToken(): void {
    const options = {expires: 1}; // 不设置 domain 属性，使 cookie 在任何域名下都有效
    Cookies.remove(TokenKey, options);
    console.log('Remove Token:', Cookies.get()); // 调试输出
}
