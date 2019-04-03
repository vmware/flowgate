import {ClaritySeedAppHome} from './app.po';

fdescribe('Flowgate app', function () {

  let expectedMsg: string = 'Welcome to VMware Flowgate';

  let page: ClaritySeedAppHome;

  beforeEach(() => {
    page = new ClaritySeedAppHome();
  });

  it('should display: ' + expectedMsg, () => {
    page.navigateTo();
    expect(page.getParagraphText()).toEqual(expectedMsg)
  });
});
