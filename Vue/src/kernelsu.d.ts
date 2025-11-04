declare module 'kernelsu' {
    export interface ExecResult {
        errno: number
        stdout: string
        stderr: string
    }

    export function exec(command: string, options?: any): Promise<ExecResult>

    export function toast(message: string): void

    export function fullScreen(isFullScreen: boolean): void
}

