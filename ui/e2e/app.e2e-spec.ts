import {ClaritySeedAppHome} from './app.po';

fdescribe('wormhole app', function () {

  let expectedMsg: string = 'Welcome to VMware Wormhole';

  let page: ClaritySeedAppHome;

  beforeEach(() => {
    page = new ClaritySeedAppHome();
  });

  it('should display: ' + expectedMsg, () => {
    page.navigateTo();
    expect(page.getParagraphText()).toEqual(expectedMsg)
  });
});
