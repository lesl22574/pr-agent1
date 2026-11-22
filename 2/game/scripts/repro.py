import asyncio, json
from playwright.async_api import async_playwright

async def main():
    async with async_playwright() as p:
        b = await p.chromium.launch()
        pg = await b.new_page(viewport={'width': 1280, 'height': 720})
        errors = []
        pg.on('pageerror', lambda e: errors.append('PAGEERROR: %s' % e))
        pg.on('console', lambda m: errors.append('CONSOLE %s: %s' % (m.type, m.text)) if m.type in ('error', 'warning') else None)
        await pg.goto('http://127.0.0.1:8000/')
        await pg.wait_for_timeout(1200)

        def coverage():
            return pg.evaluate("""() => {
                const fc = document.getElementById('foamCanvas');
                const t = document.createElement('canvas');
                t.width = t.height = 48;
                const c = t.getContext('2d');
                c.clearRect(0,0,48,48);
                c.drawImage(fc, 0, 0, 48, 48);
                const d = c.getImageData(0,0,48,48).data;
                let n = 0;
                for (let i = 3; i < d.length; i += 4) if (d[i] > 24) n++;
                return n;
            }""")

        cov0 = await coverage()
        tw0 = await pg.evaluate("document.getElementById('towelWrap').style.transform")
        await pg.mouse.move(365, 340)
        await pg.wait_for_timeout(200)
        await pg.mouse.down()
        await pg.wait_for_timeout(200)
        tw1 = await pg.evaluate("document.getElementById('towelWrap').style.transform")
        # водим по букве
        for pt in [(600, 300), (640, 360), (600, 420), (680, 300), (680, 420), (640, 250)]:
            await pg.mouse.move(pt[0], pt[1], steps=12)
            await pg.wait_for_timeout(120)
        await pg.mouse.up()
        await pg.wait_for_timeout(400)
        cov1 = await coverage()
        tw2 = await pg.evaluate("document.getElementById('towelWrap').style.transform")
        raf_alive = await pg.evaluate("""() => new Promise(res => {
            let a = 0;
            const t0 = performance.now();
            function f() { a++; if (performance.now() - t0 < 300) requestAnimationFrame(f); else res(a); }
            requestAnimationFrame(f);
        })""")
        await pg.screenshot(path='/tmp/shot_after_drag.png')
        print(json.dumps({
            'cov_before': cov0, 'cov_after': cov1,
            'towel_before': tw0, 'towel_on_press': tw1, 'towel_after': tw2,
            'raf_frames_in_300ms': raf_alive,
            'errors': errors[:10],
        }, ensure_ascii=False, indent=1))
        await b.close()

asyncio.run(main())
