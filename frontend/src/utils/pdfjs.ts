/** pdf.js 를 한 곳에서 설정한다 (뷰어·쪽 수 세기 공용) */
import { GlobalWorkerOptions, getDocument } from 'pdfjs-dist'
import workerUrl from 'pdfjs-dist/build/pdf.worker.min.mjs?url'

GlobalWorkerOptions.workerSrc = workerUrl

export { getDocument }

/** 브라우저에서 PDF 쪽 수를 센다. 열 수 없으면 null (서버가 다시 검사한다) */
export async function countPages(file: File): Promise<number | null> {
  try {
    const task = getDocument({ data: new Uint8Array(await file.arrayBuffer()) })
    const doc = await task.promise
    const n = doc.numPages
    await task.destroy()
    return n
  } catch {
    return null
  }
}
